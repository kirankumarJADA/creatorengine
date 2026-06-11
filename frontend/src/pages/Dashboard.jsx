import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Workflow,
  Zap,
  Users,
  MessageSquare,
  Plus,
  Send,
  UserPlus,
  Play,
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
import OnboardingChecklist from '../components/dashboard/OnboardingChecklist.jsx';

import { useAuthStore } from '../store/authStore.js';
import { useAutomationStore } from '../store/automationStore.js';
import dashboardService from '../services/dashboardService.js';
import { ROUTES, TRIGGER_LABEL } from '../utils/constants.js';
import { cn } from '../utils/helpers.js';

/**
 * Real-data dashboard.
 *
 *   [onboarding checklist (until all 3 steps done)]
 *   [4 stat cards across the top]
 *   [Recent activity (2/3)] [Active automations preview (1/3)]
 *
 * All counts and feed items come from the live backend — automations
 * via {@link useAutomationStore}, logs/contacts/IG status via
 * {@link dashboardService}. Empty states are honest: a brand-new
 * account sees zeros, not someone else's stats.
 */
const Dashboard = () => {
  const user = useAuthStore((s) => s.user);
  const firstName = user?.name?.split(' ')[0] || 'there';

  const automations    = useAutomationStore((s) => s.automations);
  const fetchAutomations = useAutomationStore((s) => s.fetchAutomations);

  const [isLoading, setIsLoading]   = useState(true);
  const [logs, setLogs]             = useState([]);
  const [contacts, setContacts]     = useState([]);
  const [igStatus, setIgStatus]     = useState(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setIsLoading(true);
      const [, snapshot] = await Promise.all([
        fetchAutomations(),
        dashboardService.loadAll(),
      ]);
      if (cancelled) return;
      setLogs(snapshot.logs);
      setContacts(snapshot.contacts);
      setIgStatus(snapshot.igStatus);
      setIsLoading(false);
    };

    load();
    return () => { cancelled = true; };
  }, [fetchAutomations]);

  // ─── Derived values (real, never mock) ──────────
  const igConnected     = Boolean(igStatus?.username);
  const hasAutomations  = automations.length > 0;
  const hasActivity     = logs.length > 0;

  const activeCount     = automations.filter((a) => a.enabled).length;
  const contactsCount   = contacts.length;
  const sentLast7d      = useMemo(() => countSentLast7d(logs), [logs]);

  const stats = [
    { label: 'Total automations',  value: automations.length, icon: Workflow,      tone: 'brand'   },
    { label: 'Active automations', value: activeCount,        icon: Zap,           tone: 'success' },
    { label: 'Total contacts',     value: contactsCount,      icon: Users,         tone: 'neutral' },
    { label: 'Messages sent (7d)', value: sentLast7d,         icon: MessageSquare, tone: 'warning' },
  ];

  const activeAutomations = automations.filter((a) => a.enabled).slice(0, 4);
  const recentActivity    = useMemo(() => logs.slice(0, 6).map(toActivityItem), [logs]);

  return (
    <div>
      <PageHeader
        title={`Welcome back, ${firstName}`}
        description="Here's what's happening across your workspace."
        actions={
          <Link to={ROUTES.AUTOMATION_NEW}>
            <Button leftIcon={Plus}>New automation</Button>
          </Link>
        }
      />

      {/* Onboarding (auto-hides once all 3 steps done) */}
      {!isLoading && (
        <OnboardingChecklist
          igConnected={igConnected}
          hasAutomations={hasAutomations}
          hasActivity={hasActivity}
        />
      )}

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
        <RecentActivity isLoading={isLoading} items={recentActivity} />
        <ActiveAutomationsPreview
          isLoading={isLoading}
          automations={activeAutomations}
        />
      </div>
    </div>
  );
};

// ─── Derivations ─────────────────────────────────────

const ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000;

const countSentLast7d = (logs) => {
  const cutoff = Date.now() - ONE_WEEK_MS;
  return logs.filter((l) => {
    if (!l.messageSent) return false;
    const ts = l.timestamp ? new Date(l.timestamp).getTime() : 0;
    return ts >= cutoff;
  }).length;
};

const toActivityItem = (log) => {
  const username = log.username ? `@${log.username}` : 'someone';
  const auto = log.automationName ? `"${log.automationName}"` : 'an automation';

  let type;
  let message;
  if (log.messageSent) {
    type = 'message_sent';
    message = `DM sent to ${username} from ${auto}`;
  } else if (log.matched) {
    type = 'automation_triggered';
    message = `${auto} triggered for ${username}`;
  } else {
    type = 'automation_triggered';
    message = `Event from ${username} (no match)`;
  }

  return { id: log.id, type, message, timeAgo: timeAgo(log.timestamp) };
};

const timeAgo = (iso) => {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso).getTime();
  const sec = Math.max(0, Math.floor(diff / 1000));
  if (sec < 60) return `${sec}s ago`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}m ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}h ago`;
  const day = Math.floor(hr / 24);
  return `${day}d ago`;
};

// ─── Recent activity feed ────────────────────────────

const ACTIVITY_ICON = {
  message_sent:         { icon: Send,     tone: 'bg-brand-100 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300' },
  contact_added:        { icon: UserPlus, tone: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400' },
  automation_triggered: { icon: Play,     tone: 'bg-amber-100 text-amber-800 dark:bg-amber-500/10 dark:text-amber-400' },
};

const RecentActivity = ({ isLoading, items }) => (
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
      <Link
        to="/activity"
        className="text-sm font-medium text-brand-700 hover:text-brand-800 dark:text-brand-300 dark:hover:text-brand-200"
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
    ) : items.length === 0 ? (
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
        {items.map((evt) => {
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
                  {a.condition?.keyword ? ` · "${a.condition.keyword}"` : ''}
                </p>
              </div>
              <Badge tone="success" dot>Active</Badge>
            </div>
          </li>
        ))}
      </ul>
    )}
  </Card>
);

export default Dashboard;