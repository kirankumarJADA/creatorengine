import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Users, Zap, Instagram, Workflow, Send, AlertOctagon, Activity,
} from 'lucide-react';

import PageHeader from '../../components/ui/PageHeader.jsx';
import { Card } from '../../components/ui/Card.jsx';
import StatCard from '../../components/ui/StatCard.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import adminService from '../../services/adminService.js';
import { ROUTES } from '../../utils/constants.js';
import { cn } from '../../utils/helpers.js';

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    adminService.getDashboard()
      .then((data) => { if (!cancelled) setStats(data); })
      .catch((e) => { if (!cancelled) setError(e?.message || 'Failed to load dashboard.'); })
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const cards = stats ? [
    { label: 'Total users',        value: stats.totalUsers,        icon: Users,        tone: 'brand' },
    { label: 'Active today',       value: stats.activeUsersToday,  icon: Activity,      tone: 'success' },
    { label: 'Active (7d)',        value: stats.activeUsers7d,     icon: Activity,      tone: 'success' },
    { label: 'Instagram connected', value: stats.instagramConnected, icon: Instagram,   tone: 'brand' },
    { label: 'Total automations',  value: stats.totalAutomations,  icon: Workflow,      tone: 'neutral' },
    { label: 'Active automations', value: stats.activeAutomations, icon: Zap,           tone: 'success' },
    { label: 'DMs sent',           value: stats.totalDmsSent,      icon: Send,          tone: 'brand' },
    { label: 'Failed jobs',        value: stats.failedJobsCount,   icon: AlertOctagon,  tone: 'warning' },
  ] : [];

  return (
    <div>
      <PageHeader
        title="Admin Dashboard"
        description="Platform-wide stats across every user."
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {isLoading
          ? Array.from({ length: 8 }).map((_, i) => (
              <Card key={i} className="space-y-4">
                <Skeleton className="h-10 w-10 rounded-xl" />
                <Skeleton className="h-8 w-1/3" />
                <Skeleton className="h-3 w-2/3" />
              </Card>
            ))
          : cards.map((c, i) => <StatCard key={c.label} index={i} {...c} />)}
      </div>

      {error && (
        <p className="mt-4 text-sm text-red-600 dark:text-red-400">{error}</p>
      )}

      <Card className="mt-6">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-ink-900 dark:text-ink-100">
              Recent activity
            </h3>
            <p className="text-sm text-ink-500 dark:text-ink-400">
              Latest events across all users.
            </p>
          </div>
          <Link
            to={ROUTES.ADMIN_LOGS}
            className="text-sm font-medium text-brand-700 hover:text-brand-800 dark:text-brand-300"
          >
            View all
          </Link>
        </div>

        {isLoading ? (
          <ul className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <li key={i} className="flex items-start gap-3">
                <Skeleton className="h-9 w-9 rounded-xl" />
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-3.5 w-3/4" />
                  <Skeleton className="h-3 w-1/4" />
                </div>
              </li>
            ))}
          </ul>
        ) : !stats?.recentActivity?.length ? (
          <EmptyState icon={Activity} title="No activity yet" description="Events will appear here as users interact with automations." />
        ) : (
          <ul className="divide-y divide-ink-100 dark:divide-ink-800">
            {stats.recentActivity.map((log) => (
              <li key={log.id} className="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
                <div className="min-w-0">
                  <p className="truncate text-sm text-ink-800 dark:text-ink-200">
                    {log.automationName || 'Automation'} — {log.ownerEmail || 'unknown user'}
                  </p>
                  <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">
                    {log.username ? `@${log.username}` : ''} {log.status ? `· ${log.status}` : ''}
                  </p>
                </div>
                <span className={cn(
                  'shrink-0 rounded-full px-2 py-0.5 text-xs font-medium',
                  log.messageSent
                    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400'
                    : 'bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300'
                )}>
                  {log.status || (log.matched ? 'Matched' : 'No match')}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
};

export default AdminDashboard;