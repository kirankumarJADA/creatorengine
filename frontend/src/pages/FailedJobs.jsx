import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import toast from 'react-hot-toast';
import {
  Search, AlertOctagon, RefreshCw, Trash2, RotateCcw,
  ChevronLeft, ChevronRight, AlertTriangle, Loader2,
} from 'lucide-react';

import PageHeader from '../components/ui/PageHeader.jsx';
import { Card } from '../components/ui/Card.jsx';
import Skeleton from '../components/ui/Skeleton.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import IconButton from '../components/ui/IconButton.jsx';
import Button from '../components/form/Button.jsx';

import failedJobService from '../services/failedJobService.js';
import { cn, formatRelative } from '../utils/helpers.js';
import { PAGE_SIZE } from '../utils/constants.js';

const FailedJobs = () => {
  // ─── State ───────────────────────────────────────
  const [jobs, setJobs]           = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError]         = useState(null);

  const [query, setQuery] = useState('');
  const [page, setPage]   = useState(1);

  // Per-row in-flight flags keyed by id, so the same row's two
  // buttons can each show their own spinner without blocking the
  // rest of the table.
  const [pendingRetry, setPendingRetry]   = useState({});
  const [pendingDelete, setPendingDelete] = useState({});

  // ─── Fetch ───────────────────────────────────────
  const fetchJobs = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await failedJobService.list();
      setJobs(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Failed to load failed jobs.');
      setJobs([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ─── Per-row actions ─────────────────────────────
  const handleRetry = async (job) => {
    if (pendingRetry[job.id] || pendingDelete[job.id]) return;
    setPendingRetry((p) => ({ ...p, [job.id]: true }));
    try {
      await failedJobService.retry(job.id);
      // Successful retry deletes the server-side row; reflect locally
      // so the user sees the row vanish immediately.
      setJobs((prev) => prev.filter((j) => j.id !== job.id));
      toast.success('Retry queued.');
    } catch (err) {
      const msg = err?.response?.data?.message || 'Could not queue retry.';
      toast.error(msg);
    } finally {
      setPendingRetry((p) => { const { [job.id]: _, ...rest } = p; return rest; });
    }
  };

  const handleDelete = async (job) => {
    if (pendingRetry[job.id] || pendingDelete[job.id]) return;
    if (!window.confirm('Permanently discard this failed job?')) return;

    setPendingDelete((p) => ({ ...p, [job.id]: true }));
    try {
      await failedJobService.delete(job.id);
      setJobs((prev) => prev.filter((j) => j.id !== job.id));
      toast.success('Failed job deleted.');
    } catch (err) {
      const msg = err?.response?.data?.message || 'Could not delete.';
      toast.error(msg);
    } finally {
      setPendingDelete((p) => { const { [job.id]: _, ...rest } = p; return rest; });
    }
  };

  // ─── Filter ──────────────────────────────────────
  // Search covers username + automationName + reason — those are the
  // free-text fields a user would scan for.
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return jobs;
    return jobs.filter((j) => {
      const haystack = [
        j.username, j.automationName, j.reason,
      ].filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(q);
    });
  }, [jobs, query]);

  // Clamp page when filter shrinks the result set
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [totalPages, page]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  // ─── Render ──────────────────────────────────────
  return (
    <div>
      <PageHeader
        title="Failed Jobs"
        description="Automation runs that gave up after exhausting retries — review, retry, or discard."
        actions={
          <Button variant="secondary" leftIcon={RefreshCw} onClick={fetchJobs}>
            Refresh
          </Button>
        }
      />

      {/* ─── Filter bar ──────────────────────────────── */}
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search
            size={16}
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400"
          />
          <input
            type="search"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setPage(1); }}
            placeholder="Search by username, automation, or reason…"
            className="input pl-9"
          />
        </div>
      </div>

      {/* ─── Error banner ────────────────────────────── */}
      {error && !isLoading && (
        <div className="mb-4 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 dark:border-red-500/30 dark:bg-red-500/10">
          <AlertTriangle size={18} className="mt-0.5 shrink-0 text-red-600 dark:text-red-400" />
          <div className="min-w-0 flex-1 text-sm">
            <p className="font-semibold text-red-900 dark:text-red-200">
              Couldn’t load failed jobs
            </p>
            <p className="mt-0.5 text-red-800 dark:text-red-300/90">{error}</p>
          </div>
          <Button size="sm" variant="secondary" onClick={fetchJobs}>
            Retry
          </Button>
        </div>
      )}

      {/* ─── Table or empty state ────────────────────── */}
      <Card padded={false} className="overflow-hidden">
        {isLoading ? (
          <TableSkeleton />
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={AlertOctagon}
            title={
              query                ? 'No matches'
              : error              ? 'No failed jobs to show'
              :                      'No failed jobs 🎉'
            }
            description={
              query
                ? 'Try a different search or clear the box.'
                : error
                ? 'Try refreshing once the issue above is resolved.'
                : 'Your automations are all running cleanly. Failures will land here when something gives up after retries.'
            }
            className="border-none"
          />
        ) : (
          <FailedJobsTable
            rows={paginated}
            pendingRetry={pendingRetry}
            pendingDelete={pendingDelete}
            onRetry={handleRetry}
            onDelete={handleDelete}
          />
        )}

        {!isLoading && filtered.length > 0 && (
          <Pagination
            page={page}
            totalPages={totalPages}
            total={filtered.length}
            pageSize={PAGE_SIZE}
            onChange={setPage}
          />
        )}
      </Card>
    </div>
  );
};

// ─── Table ─────────────────────────────────────────
const FailedJobsTable = ({ rows, pendingRetry, pendingDelete, onRetry, onDelete }) => (
  <div className="overflow-x-auto">
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-ink-100 bg-ink-50/60 text-left text-xs uppercase tracking-wider text-ink-500 dark:border-ink-800 dark:bg-ink-900/60 dark:text-ink-400">
          <th className="px-5 py-3 font-semibold">Username</th>
          <th className="px-5 py-3 font-semibold">Automation</th>
          <th className="px-5 py-3 font-semibold">Reason</th>
          <th className="px-5 py-3 font-semibold">Attempts</th>
          <th className="px-5 py-3 font-semibold">Timestamp</th>
          <th className="px-5 py-3 text-right font-semibold">Actions</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => {
          const isRetrying = !!pendingRetry[row.id];
          const isDeleting = !!pendingDelete[row.id];
          const busy = isRetrying || isDeleting;
          return (
            <motion.tr
              key={row.id || i}
              initial={{ opacity: 0, y: 4 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.2, delay: Math.min(i * 0.02, 0.3) }}
              className={cn(
                'border-b border-ink-100 last:border-0 transition-colors',
                'hover:bg-ink-50/60 dark:border-ink-800 dark:hover:bg-ink-800/40',
                busy && 'opacity-60'
              )}
            >
              <td className="px-5 py-3.5 font-mono text-ink-900 dark:text-ink-100">
                {row.username ? '@' + row.username : '—'}
              </td>
              <td className="px-5 py-3.5 text-ink-800 dark:text-ink-200">
                {row.automationName || (
                  <span className="italic text-ink-400">—</span>
                )}
              </td>
              <td
                className="max-w-sm truncate px-5 py-3.5 text-ink-600 dark:text-ink-400"
                title={row.reason || ''}
              >
                {row.reason || '—'}
              </td>
              <td className="px-5 py-3.5 font-mono text-ink-800 dark:text-ink-200">
                {row.attempts ?? 0}
              </td>
              <td
                className="px-5 py-3.5 text-ink-500 dark:text-ink-400"
                title={row.timestamp || ''}
              >
                {row.timestamp ? formatRelative(row.timestamp) : '—'}
              </td>
              <td className="px-5 py-3.5">
                <div className="flex items-center justify-end gap-1">
                  <IconButton
                    aria-label="Retry this job"
                    title="Retry"
                    onClick={() => onRetry(row)}
                    disabled={busy}
                  >
                    {isRetrying
                      ? <Loader2 size={16} className="animate-spin" />
                      : <RotateCcw size={16} />}
                  </IconButton>
                  <IconButton
                    aria-label="Delete this job"
                    title="Delete"
                    onClick={() => onDelete(row)}
                    disabled={busy}
                  >
                    {isDeleting
                      ? <Loader2 size={16} className="animate-spin" />
                      : <Trash2 size={16} />}
                  </IconButton>
                </div>
              </td>
            </motion.tr>
          );
        })}
      </tbody>
    </table>
  </div>
);

// ─── Skeleton (mirrors Activity Logs pattern) ─────
const TableSkeleton = () => (
  <div className="overflow-x-auto">
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-ink-100 bg-ink-50/60 dark:border-ink-800 dark:bg-ink-900/60">
          {['Username', 'Automation', 'Reason', 'Attempts', 'Timestamp', 'Actions'].map((h, i) => (
            <th
              key={h}
              className={cn(
                'px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-ink-500 dark:text-ink-400',
                i === 5 && 'text-right'
              )}
            >
              {h}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {Array.from({ length: 6 }).map((_, i) => (
          <tr key={i} className="border-b border-ink-100 last:border-0 dark:border-ink-800">
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-24" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-32" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-56" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-8" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-20" /></td>
            <td className="px-5 py-3.5">
              <div className="flex justify-end gap-1">
                <Skeleton className="h-8 w-8 rounded-full" />
                <Skeleton className="h-8 w-8 rounded-full" />
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

// ─── Pagination (identical pattern to ActivityLogs/Contacts) ──
const Pagination = ({ page, totalPages, total, pageSize, onChange }) => {
  const first = (page - 1) * pageSize + 1;
  const last  = Math.min(page * pageSize, total);
  return (
    <nav
      aria-label="Pagination"
      className="flex flex-col items-center justify-between gap-3 border-t border-ink-100 px-5 py-3 text-sm dark:border-ink-800 sm:flex-row"
    >
      <p className="text-ink-500 dark:text-ink-400">
        Showing <span className="font-medium text-ink-800 dark:text-ink-200">{first}–{last}</span>{' '}
        of {total}
      </p>
      <div className="flex items-center gap-1">
        <IconButton
          aria-label="Previous page"
          onClick={() => onChange(Math.max(1, page - 1))}
          disabled={page <= 1}
        >
          <ChevronLeft size={16} />
        </IconButton>
        <span className="px-3 text-sm font-medium text-ink-800 dark:text-ink-200">
          {page} <span className="text-ink-400">/ {totalPages}</span>
        </span>
        <IconButton
          aria-label="Next page"
          onClick={() => onChange(Math.min(totalPages, page + 1))}
          disabled={page >= totalPages}
        >
          <ChevronRight size={16} />
        </IconButton>
      </div>
    </nav>
  );
};

export default FailedJobs;
