import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Search, ScrollText,
  ChevronLeft, ChevronRight, AlertTriangle, RefreshCw,
} from 'lucide-react';

import PageHeader from '../components/ui/PageHeader.jsx';
import { Card } from '../components/ui/Card.jsx';
import Badge from '../components/ui/Badge.jsx';
import Skeleton from '../components/ui/Skeleton.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import Dropdown from '../components/ui/Dropdown.jsx';
import IconButton from '../components/ui/IconButton.jsx';
import Button from '../components/form/Button.jsx';

import logService from '../services/logService.js';
import { cn, formatRelative } from '../utils/helpers.js';
import { PAGE_SIZE, LOG_STATUS } from '../utils/constants.js';

// ─── Status filter options + visual treatment ────────
const STATUS_FILTERS = [
  { value: 'all',                          label: 'All statuses'  },
  { value: LOG_STATUS.SUCCESS,             label: 'Success'       },
  { value: LOG_STATUS.FAILED,              label: 'Failed'        },
  { value: LOG_STATUS.COOLDOWN_SKIPPED,    label: 'Cooldown'      },
  { value: LOG_STATUS.DUPLICATE_IGNORED,   label: 'Duplicate'     },
];

const STATUS_LABEL = {
  [LOG_STATUS.SUCCESS]:           'Success',
  [LOG_STATUS.FAILED]:            'Failed',
  [LOG_STATUS.COOLDOWN_SKIPPED]:  'Cooldown',
  [LOG_STATUS.DUPLICATE_IGNORED]: 'Duplicate',
};

const STATUS_TONE = {
  [LOG_STATUS.SUCCESS]:           'success',
  [LOG_STATUS.FAILED]:            'danger',
  [LOG_STATUS.COOLDOWN_SKIPPED]:  'warning',
  [LOG_STATUS.DUPLICATE_IGNORED]: 'neutral',
};

// Render-friendly trigger labels (backend stores enum names).
const TRIGGER_LABEL = {
  COMMENT:     'Comment',
  DM:          'DM',
  STORY_REPLY: 'Story',
};

const ActivityLogs = () => {
  // ─── State ───────────────────────────────────────
  const [logs, setLogs]           = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError]         = useState(null);

  const [query, setQuery]                   = useState('');
  const [statusFilter, setStatusFilter]     = useState('all');
  const [automationFilter, setAutomationFilter] = useState('all');
  const [dateFrom, setDateFrom]             = useState('');
  const [dateTo, setDateTo]                 = useState('');
  const [page, setPage]                     = useState(1);

  // ─── Fetch ───────────────────────────────────────
  const fetchLogs = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await logService.list();
      setLogs(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Failed to load activity logs.');
      setLogs([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ─── Build automation filter options dynamically from data ───
  // Empty/missing automation names (DUPLICATE_IGNORED rows have none)
  // are bucketed under a sentinel option so they don't get lost when
  // the user picks "All".
  const automationOptions = useMemo(() => {
    const seen = new Set();
    for (const l of logs) {
      if (l.automationName) seen.add(l.automationName);
    }
    const opts = [{ value: 'all', label: 'All automations' }];
    Array.from(seen).sort().forEach((name) => opts.push({ value: name, label: name }));
    return opts;
  }, [logs]);

  // ─── Filter ──────────────────────────────────────
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const from = dateFrom ? new Date(dateFrom + 'T00:00:00').getTime() : null;
    const to   = dateTo   ? new Date(dateTo   + 'T23:59:59.999').getTime() : null;

    return logs.filter((l) => {
      if (statusFilter !== 'all' && l.status !== statusFilter) return false;
      if (automationFilter !== 'all' && l.automationName !== automationFilter) return false;
      if (q && !(l.username || '').toLowerCase().includes(q)) return false;
      if (from || to) {
        const t = l.timestamp ? new Date(l.timestamp).getTime() : null;
        if (t == null) return false;
        if (from && t < from) return false;
        if (to   && t > to)   return false;
      }
      return true;
    });
  }, [logs, query, statusFilter, automationFilter, dateFrom, dateTo]);

  // Clamp page when filters change
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [totalPages, page]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  const hasActiveFilters =
    query.trim() !== '' ||
    statusFilter !== 'all' ||
    automationFilter !== 'all' ||
    dateFrom !== '' ||
    dateTo !== '';

  // ─── Render ──────────────────────────────────────
  return (
    <div>
      <PageHeader
        title="Activity Logs"
        description="What your automations have been doing — successes, failures, skips, and duplicates."
        actions={
          <Button variant="secondary" leftIcon={RefreshCw} onClick={fetchLogs}>
            Refresh
          </Button>
        }
      />

      {/* ─── Filter bar ──────────────────────────────── */}
      <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <div className="relative sm:col-span-2 lg:col-span-2">
          <Search
            size={16}
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400"
          />
          <input
            type="search"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setPage(1); }}
            placeholder="Search by username…"
            className="input pl-9 w-full"
          />
        </div>
        <Dropdown
          label="Status"
          options={STATUS_FILTERS}
          value={statusFilter}
          onChange={(v) => { setStatusFilter(v); setPage(1); }}
        />
        <Dropdown
          label="Automation"
          options={automationOptions}
          value={automationFilter}
          onChange={(v) => { setAutomationFilter(v); setPage(1); }}
        />
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => { setDateFrom(e.target.value); setPage(1); }}
            className="input min-w-0 flex-1"
            aria-label="From date"
          />
          <span className="text-ink-400">–</span>
          <input
            type="date"
            value={dateTo}
            onChange={(e) => { setDateTo(e.target.value); setPage(1); }}
            className="input min-w-0 flex-1"
            aria-label="To date"
          />
        </div>
      </div>

      {/* ─── Error banner ────────────────────────────── */}
      {error && !isLoading && (
        <div className="mb-4 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 dark:border-red-500/30 dark:bg-red-500/10">
          <AlertTriangle size={18} className="mt-0.5 shrink-0 text-red-600 dark:text-red-400" />
          <div className="min-w-0 flex-1 text-sm">
            <p className="font-semibold text-red-900 dark:text-red-200">
              Couldn’t load activity logs
            </p>
            <p className="mt-0.5 text-red-800 dark:text-red-300/90">{error}</p>
          </div>
          <Button size="sm" variant="secondary" onClick={fetchLogs}>
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
            icon={ScrollText}
            title={hasActiveFilters ? 'No matches' :
                   error ? 'No activity to show' : 'No activity yet'}
            description={
              hasActiveFilters
                ? 'Try a different search or clear the filters.'
                : error
                ? 'Try refreshing once the issue above is resolved.'
                : 'Logs appear here as your automations process incoming events.'
            }
            className="border-none"
          />
        ) : (
          <LogsTable rows={paginated} />
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
const LogsTable = ({ rows }) => (
  <div className="overflow-x-auto">
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-ink-100 bg-ink-50/60 text-left text-xs uppercase tracking-wider text-ink-500 dark:border-ink-800 dark:bg-ink-900/60 dark:text-ink-400">
          <th className="px-5 py-3 font-semibold">Username</th>
          <th className="px-5 py-3 font-semibold">Automation</th>
          <th className="px-5 py-3 font-semibold">Trigger</th>
          <th className="px-5 py-3 font-semibold">Event text</th>
          <th className="px-5 py-3 font-semibold">Status</th>
          <th className="px-5 py-3 font-semibold">Timestamp</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <motion.tr
            key={row.id || i}
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.2, delay: Math.min(i * 0.02, 0.3) }}
            className={cn(
              'border-b border-ink-100 last:border-0 transition-colors',
              'hover:bg-ink-50/60 dark:border-ink-800 dark:hover:bg-ink-800/40'
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
            <td className="px-5 py-3.5 text-ink-700 dark:text-ink-300">
              {TRIGGER_LABEL[row.triggerType] || row.triggerType || '—'}
            </td>
            <td
              className="max-w-xs truncate px-5 py-3.5 text-ink-600 dark:text-ink-400"
              title={row.eventText || ''}
            >
              {row.eventText || '—'}
            </td>
            <td className="px-5 py-3.5">
              <Badge tone={STATUS_TONE[row.status] || 'neutral'} dot>
                {STATUS_LABEL[row.status] || row.status || '—'}
              </Badge>
            </td>
            <td
              className="px-5 py-3.5 text-ink-500 dark:text-ink-400"
              title={row.timestamp || ''}
            >
              {row.timestamp ? formatRelative(row.timestamp) : '—'}
            </td>
          </motion.tr>
        ))}
      </tbody>
    </table>
  </div>
);

// ─── Skeleton (mirrors Contacts page) ─────────────
const TableSkeleton = () => (
  <div className="overflow-x-auto">
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-ink-100 bg-ink-50/60 dark:border-ink-800 dark:bg-ink-900/60">
          {['Username', 'Automation', 'Trigger', 'Event text', 'Status', 'Timestamp'].map((h) => (
            <th key={h} className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-ink-500 dark:text-ink-400">
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
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-16" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-44" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-5 w-20 rounded-full" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-20" /></td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

// ─── Pagination (identical pattern to Contacts.jsx) ───
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

export default ActivityLogs;
