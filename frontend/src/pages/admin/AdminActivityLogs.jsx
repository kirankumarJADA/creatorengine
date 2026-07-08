import { useEffect, useMemo, useState } from 'react';
import { Search, ScrollText } from 'lucide-react';

import PageHeader from '../../components/ui/PageHeader.jsx';
import { Card } from '../../components/ui/Card.jsx';
import Badge from '../../components/ui/Badge.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import adminService from '../../services/adminService.js';
import { formatRelative } from '../../utils/helpers.js';

const AdminActivityLogs = () => {
  const [logs, setLogs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [query, setQuery] = useState('');

  useEffect(() => {
    let cancelled = false;
    adminService.listLogs(300)
      .then((data) => { if (!cancelled) setLogs(data); })
      .catch(() => {})
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return logs;
    return logs.filter((l) =>
      (l.ownerEmail || '').toLowerCase().includes(q) ||
      (l.username || '').toLowerCase().includes(q) ||
      (l.automationName || '').toLowerCase().includes(q)
    );
  }, [logs, query]);

  const statusTone = (status) => {
    if (status === 'SUCCESS') return 'success';
    if (status === 'FAILED') return 'danger';
    return 'neutral';
  };

  return (
    <div>
      <PageHeader title="Activity Logs" description="Every automation event, across every user." />

      <div className="relative mb-6 max-w-md">
        <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by user, username, or automation…"
          className="input pl-9"
        />
      </div>

      <Card padded={false}>
        {isLoading ? (
          <div className="space-y-3 p-5">
            {Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
          </div>
        ) : filtered.length === 0 ? (
          <div className="p-8">
            <EmptyState icon={ScrollText} title="No logs found" description="Try a different search." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-ink-100 text-left text-xs uppercase tracking-wider text-ink-400 dark:border-ink-800">
                  <th className="px-5 py-3 font-medium">Owner</th>
                  <th className="px-5 py-3 font-medium">Automation</th>
                  <th className="px-5 py-3 font-medium">Recipient</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium">When</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100 dark:divide-ink-800">
                {filtered.map((l) => (
                  <tr key={l.id} className="row-hover">
                    <td className="px-5 py-3 text-ink-800 dark:text-ink-200">{l.ownerEmail || '—'}</td>
                    <td className="px-5 py-3 text-ink-600 dark:text-ink-300">{l.automationName || '—'}</td>
                    <td className="px-5 py-3 text-ink-600 dark:text-ink-300">
                      {l.username ? `@${l.username}` : '—'}
                    </td>
                    <td className="px-5 py-3">
                      {l.status ? (
                        <Badge tone={statusTone(l.status)} dot>{l.status}</Badge>
                      ) : (
                        <span className="text-xs text-ink-400">—</span>
                      )}
                    </td>
                    <td className="px-5 py-3 text-ink-500 dark:text-ink-400">{formatRelative(l.timestamp)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
};

export default AdminActivityLogs;