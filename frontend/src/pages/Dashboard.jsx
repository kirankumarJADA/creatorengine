import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Workflow,
  Zap,
  Users,
  MessageSquare,
  Plus,
  ArrowUpRight,
  Send,
  UserPlus,
  Play,
  PauseCircle,
  Inbox,
} from 'lucide-react';
import { motion } from 'framer-motion';

import PageHeader from '../components/ui/PageHeader.jsx';
import { Card } from '../components/ui/Card.jsx';
import Badge from '../components/ui/Badge.jsx';
import StatCard from '../components/ui/StatCard.jsx';
import Skeleton from '../components/ui/Skeleton.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import Button from '../components/form/Button.jsx';

import { useAuthStore } from '../store/authStore.js';
import { ROUTES, TRIGGER_LABEL } from '../utils/constants.js';
import { cn } from '../utils/helpers.js';
import {
  mockStats,
  mockAutomations,
  mockActivity,
} from '../utils/mockData.js';

/**
 * Analytics-style dashboard.
 *
 * Layout:
 *   [4 stat cards across the top]
 *   [Recent activity (2/3)]     [Active automations preview (1/3)]
 *
 * The page simulates a network fetch on mount so the skeleton state
 * is visible long enough to verify it works — swap the timeout for
 * real API calls once the backend resources exist.
 */
const Dashboard = () => {
  const user = useAuthStore((s) => s.user);
  const firstName = user?.name?.split(' ')[0] || 'there';

  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const t = setTimeout(() => setIsLoading(false), 700);
    return () => clearTimeout(t);
  }, []);

  const stats = [
    { label: 'Total automations',  value: mockStats.totalAutomations,  icon: Workflow,       tone: 'brand',   delta: mockStats.deltas.totalAutomations },
    { label: 'Active automations', value: mockStats.activeAutomations, icon: Zap,            tone: 'success', delta: mockStats.deltas.activeAutomations },
    { label: 'Total contacts',     value: mockStats.totalContacts,     icon: Users,          tone: 'neutral', delta: mockStats.deltas.totalContacts },
    { label: 'Messages sent',      value: mockStats.messagesSent,      icon: MessageSquare,  tone: 'warning', delta: mockStats.deltas.messagesSent },
  ];

  const activeAutomations = mockAutomations
    .filter((a) => a.enabled)
    .slice(0, 4);

  return (
    <div>
      <PageHeader
        title={`Welcome back, ${firstName}`}
        description="Here’s what’s happening across your workspace today."
        actions={
          <Link to={ROUTES.AUTOMATION_NEW}>
            <Button leftIcon={Plus}>New automation</Button>
          </Link>
        }
      />

      {/* ─── Stats ──────────────────────────────────── */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {isLoading
          ? Array.from({ length: 4 }).map((_, i) => (
              <Card key={i} className="space-y-4">
                <Skeleton className="h-10 w-10 rounded-xl" />
                <Skeleton className="h-8 w-1/3" />
                <Skeleton className="h-3 w-2/3" />
              </Card>
            ))
          : stats.map((s, i) => <StatCard key={s.label} index={i} {...s} />)}
      </div>

      {/* ─── Activity + automations ─────────────────── */}
      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3 lg:mt-8">
        <RecentActivity isLoading={isLoading} />
        <ActiveAutomationsPreview
          isLoading={isLoading}
          automations={activeAutomations}
        />
      </div>
    </div>
  );
};

// ─── Recent activity feed ────────────────────────────
const ACTIVITY_ICON = {
  message_sent:         { icon: Send,         tone: 'bg-brand-100 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300' },
  contact_added:        { icon: UserPlus,     tone: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400' },
  automation_triggered: { icon: Play,         tone: 'bg-amber-100 text-amber-800 dark:bg-amber-500/10 dark:text-amber-400' },
  automation_paused:    { icon: PauseCircle,  tone: 'bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-400' },
};

const RecentActivity = ({ isLoading }) => (
  <Card className="lg:col-span-2">
    <div className="mb-4 flex items-center justify-between">
      <div>
        <h3 className="text-lg font-semibold text-ink-900 dark:text-ink-100">
          Recent activity
        </h3>
        <p className="text-sm text-ink-500 dark:text-ink-400">
          Live events from your workspace.
        </p>
      </div>
      <Button variant="ghost" size="sm">View all</Button>
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
    ) : mockActivity.length === 0 ? (
      <EmptyState
        icon={Inbox}
        title="No activity yet"
        description="Once your automations start running, events will show up here."
      />
    ) : (
      <motion.ul
        initial="hidden"
        animate="visible"
        variants={{
          hidden:  {},
          visible: { transition: { staggerChildren: 0.04 } },
        }}
        className="divide-y divide-ink-100 dark:divide-ink-800"
      >
        {mockActivity.map((evt) => {
          const meta = ACTIVITY_ICON[evt.type] || ACTIVITY_ICON.message_sent;
          const Icon = meta.icon;
          return (
            <motion.li
              key={evt.id}
              variants={{ hidden: { opacity: 0, y: 4 }, visible: { opacity: 1, y: 0 } }}
              className="flex items-start gap-3 py-3 first:pt-0 last:pb-0"
            >
              <span className={cn('grid h-9 w-9 shrink-0 place-items-center rounded-xl', meta.tone)}>
                <Icon size={16} />
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm text-ink-800 dark:text-ink-200">{evt.message}</p>
                <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">{evt.timeAgo}</p>
              </div>
            </motion.li>
          );
        })}
      </motion.ul>
    )}
  </Card>
);

// ─── Active automations preview ──────────────────────
const ActiveAutomationsPreview = ({ isLoading, automations }) => (
  <Card>
    <div className="mb-4 flex items-center justify-between">
      <div>
        <h3 className="text-lg font-semibold text-ink-900 dark:text-ink-100">
          Active automations
        </h3>
        <p className="text-sm text-ink-500 dark:text-ink-400">Running right now.</p>
      </div>
      <Link
        to={ROUTES.AUTOMATIONS}
        className="text-sm font-medium text-brand-700 hover:text-brand-800 dark:text-brand-300 dark:hover:text-brand-200"
      >
        See all
      </Link>
    </div>

    {isLoading ? (
      <ul className="space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <li key={i} className="rounded-xl border border-ink-100 p-3 dark:border-ink-800">
            <Skeleton className="h-3.5 w-2/3" />
            <Skeleton className="mt-2 h-3 w-1/2" />
          </li>
        ))}
      </ul>
    ) : automations.length === 0 ? (
      <EmptyState
        icon={Zap}
        title="Nothing running"
        description="Activate an automation to see it here."
        className="py-10"
      />
    ) : (
      <ul className="space-y-2">
        {automations.map((a) => (
          <li
            key={a.id}
            className="group rounded-xl border border-ink-100 p-3 transition-colors hover:border-ink-200 dark:border-ink-800 dark:hover:border-ink-700"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-ink-900 dark:text-ink-100">
                  {a.name}
                </p>
                <p className="mt-0.5 truncate text-xs text-ink-500 dark:text-ink-400">
                  {TRIGGER_LABEL[a.trigger] || a.trigger}
                  {a.condition?.keyword ? ` · “${a.condition.keyword}”` : ''}
                </p>
              </div>
              <Badge tone="success" dot>Active</Badge>
            </div>
            <p className="mt-3 text-xs text-ink-500 dark:text-ink-400">
              {a.runCount.toLocaleString()} runs
              <span className="mx-1.5">·</span>
              <ArrowUpRight size={11} className="inline -translate-y-px text-emerald-500" />
              {Math.round((a.successCount / Math.max(1, a.runCount)) * 100)}% success
            </p>
          </li>
        ))}
      </ul>
    )}
  </Card>
);

export default Dashboard;
