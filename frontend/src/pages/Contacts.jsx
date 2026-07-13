import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Plus, Search, Upload, Users,
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

import contactService from '../services/contactService.js';
import { useAccountStore } from '../store/accountStore.js';
import { cn, formatRelative, formatDate, getInitials } from '../utils/helpers.js';
import { PAGE_SIZE } from '../utils/constants.js';

const SOURCE_FILTERS = [
  { value: 'all',         label: 'All sources' },
  { value: 'COMMENT',     label: 'Comment'      },
  { value: 'DM',          label: 'DM'           },
  { value: 'STORY_REPLY', label: 'Story'        },
];

const SOURCE_LABEL = { COMMENT: 'Comment', DM: 'DM', STORY_REPLY: 'Story' };
const SOURCE_TONE = { COMMENT: 'brand', DM: 'success', STORY_REPLY: 'warning' };

const Contacts = () => {
  const [contacts, setContacts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const [query, setQuery] = useState('');
  const [sourceFilter, setSourceFilter] = useState('all');
  const [page, setPage] = useState(1);

  // Subscribe to active account — re-fetch when it changes
  const activeAccount = useAccountStore((s) => s.activeAccount);
  const activeIgId = activeAccount?.instagramUserId;

  const fetchContacts = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await contactService.list();
      setContacts(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Failed to load contacts.');
      setContacts([]);
    } finally {
      setIsLoading(false);
    }
  };

  // KEY FIX: clear stale data + re-fetch whenever the active account changes
  useEffect(() => {
    setContacts([]);
    setPage(1);
    setQuery('');
    if (activeIgId) {
      fetchContacts();
    } else {
      setIsLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeIgId]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return contacts.filter((c) => {
      if (sourceFilter !== 'all' && c.source !== sourceFilter) return false;
      if (!q) return true;
      return (c.username || '').toLowerCase().includes(q);
    });
  }, [contacts, query, sourceFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [totalPages, page]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  if (!activeIgId) {
    return (
      <div>
        <PageHeader title="Contacts" description="People who've interacted with your account." />
        <EmptyState
          icon={Users}
          title="No account selected"
          description="Select an Instagram account from the sidebar to view contacts."
        />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Contacts"
        description="People who've interacted with your account."
        actions={
          <div className="flex gap-2">
            <Button variant="secondary" leftIcon={RefreshCw} onClick={fetchContacts}>
              Refresh
            </Button>
            <Button variant="secondary" leftIcon={Upload}>Import</Button>
            <Button leftIcon={Plus}>Add contact</Button>
          </div>
        }
      />

      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
          <input
            type="search"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setPage(1); }}
            placeholder="Search by username…"
            className="input pl-9"
          />
        </div>
        <Dropdown
          label="Source"
          options={SOURCE_FILTERS}
          value={sourceFilter}
          onChange={(v) => { setSourceFilter(v); setPage(1); }}
          align="right"
        />
      </div>

      {error && !isLoading && (
        <div className="mb-4 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 dark:border-red-500/30 dark:bg-red-500/10">
          <AlertTriangle size={18} className="mt-0.5 shrink-0 text-red-600 dark:text-red-400" />
          <div className="min-w-0 flex-1 text-sm">
            <p className="font-semibold text-red-900 dark:text-red-200">Couldn't load contacts</p>
            <p className="mt-0.5 text-red-800 dark:text-red-300/90">{error}</p>
          </div>
          <Button size="sm" variant="secondary" onClick={fetchContacts}>Retry</Button>
        </div>
      )}

      <Card padded={false} className="overflow-hidden">
        {isLoading ? (
          <TableSkeleton />
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={Users}
            title={query || sourceFilter !== 'all' ? 'No matches' : error ? 'No contacts to show' : 'No contacts yet'}
            description={
              query || sourceFilter !== 'all'
                ? 'Try a different search or clear the filter.'
                : error
                ? 'Try refreshing once the issue above is resolved.'
                : 'Contacts will appear here as your automations engage with people.'
            }
            className="border-none"
          />
        ) : (
          <ContactsTable contacts={paginated} />
        )}

        {!isLoading && filtered.length > 0 && (
          <Pagination page={page} totalPages={totalPages} total={filtered.length} pageSize={PAGE_SIZE} onChange={setPage} />
        )}
      </Card>
    </div>
  );
};

const ContactsTable = ({ contacts }) => (
  <div className="overflow-x-auto">
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-ink-100 bg-ink-50/60 text-left text-xs uppercase tracking-wider text-ink-500 dark:border-ink-800 dark:bg-ink-900/60 dark:text-ink-400">
          <th className="px-5 py-3 font-semibold">Contact</th>
          <th className="px-5 py-3 font-semibold">Instagram ID</th>
          <th className="px-5 py-3 font-semibold">Source</th>
          <th className="px-5 py-3 font-semibold">Triggers</th>
          <th className="px-5 py-3 font-semibold">Last interaction</th>
          <th className="px-5 py-3 font-semibold">Created</th>
        </tr>
      </thead>
      <tbody>
        {contacts.map((c, i) => (
          <motion.tr
            key={c.id || c.instagramUserId}
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.2, delay: Math.min(i * 0.02, 0.3) }}
            className={cn('border-b border-ink-100 last:border-0 transition-colors', 'hover:bg-ink-50/60 dark:border-ink-800 dark:hover:bg-ink-800/40')}
          >
            <td className="px-5 py-3.5">
              <div className="flex items-center gap-3">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-brand-100 text-xs font-semibold text-brand-700 dark:bg-brand-500/15 dark:text-brand-300">
                  {getInitials(c.username || '?')}
                </span>
                <span className="font-mono font-medium text-ink-900 dark:text-ink-100">@{c.username || '—'}</span>
              </div>
            </td>
            <td className="px-5 py-3.5 font-mono text-xs text-ink-600 dark:text-ink-400" title={c.instagramUserId}>
              {truncateId(c.instagramUserId)}
            </td>
            <td className="px-5 py-3.5">
              <Badge tone={SOURCE_TONE[c.source] || 'neutral'} dot>{SOURCE_LABEL[c.source] || c.source || '—'}</Badge>
            </td>
            <td className="px-5 py-3.5 font-mono text-ink-800 dark:text-ink-200">{(c.totalTriggers ?? 0).toLocaleString()}</td>
            <td className="px-5 py-3.5 text-ink-700 dark:text-ink-300">{c.lastInteraction ? formatRelative(c.lastInteraction) : '—'}</td>
            <td className="px-5 py-3.5 text-ink-500 dark:text-ink-400">{c.createdAt ? formatDate(c.createdAt) : '—'}</td>
          </motion.tr>
        ))}
      </tbody>
    </table>
  </div>
);

const truncateId = (id) => {
  if (!id) return '—';
  if (id.length <= 12) return id;
  return id.slice(0, 6) + '…' + id.slice(-4);
};

const TableSkeleton = () => (
  <div className="overflow-x-auto">
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-ink-100 bg-ink-50/60 dark:border-ink-800 dark:bg-ink-900/60">
          {['Contact', 'Instagram ID', 'Source', 'Triggers', 'Last interaction', 'Created'].map((h) => (
            <th key={h} className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-ink-500 dark:text-ink-400">{h}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {Array.from({ length: 6 }).map((_, i) => (
          <tr key={i} className="border-b border-ink-100 last:border-0 dark:border-ink-800">
            <td className="px-5 py-3.5">
              <div className="flex items-center gap-3">
                <Skeleton className="h-8 w-8 rounded-full" />
                <Skeleton className="h-3.5 w-28" />
              </div>
            </td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-24" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-5 w-20 rounded-full" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-12" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-20" /></td>
            <td className="px-5 py-3.5"><Skeleton className="h-3.5 w-24" /></td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

const Pagination = ({ page, totalPages, total, pageSize, onChange }) => {
  const first = (page - 1) * pageSize + 1;
  const last = Math.min(page * pageSize, total);
  return (
    <nav aria-label="Pagination" className="flex flex-col items-center justify-between gap-3 border-t border-ink-100 px-5 py-3 text-sm dark:border-ink-800 sm:flex-row">
      <p className="text-ink-500 dark:text-ink-400">
        Showing <span className="font-medium text-ink-800 dark:text-ink-200">{first}–{last}</span> of {total}
      </p>
      <div className="flex items-center gap-1">
        <IconButton aria-label="Previous page" onClick={() => onChange(Math.max(1, page - 1))} disabled={page <= 1}><ChevronLeft size={16} /></IconButton>
        <span className="px-3 text-sm font-medium text-ink-800 dark:text-ink-200">{page} <span className="text-ink-400">/ {totalPages}</span></span>
        <IconButton aria-label="Next page" onClick={() => onChange(Math.min(totalPages, page + 1))} disabled={page >= totalPages}><ChevronRight size={16} /></IconButton>
      </div>
    </nav>
  );
};

export default Contacts;