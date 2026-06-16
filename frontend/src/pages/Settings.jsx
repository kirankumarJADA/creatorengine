import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import authService from '../services/authService.js';
import toast from 'react-hot-toast';
import { useSearchParams } from 'react-router-dom';
import {
  User,
  Lock,
  Bell,
  Instagram,
  Mail,
  KeyRound,
  Loader2,
  RefreshCw,
  Link2Off,
  AlertTriangle,
} from 'lucide-react';

import PageHeader from '../components/ui/PageHeader.jsx';
import { Card, CardHeader } from '../components/ui/Card.jsx';
import Badge from '../components/ui/Badge.jsx';
import Switch from '../components/ui/Switch.jsx';
import Button from '../components/form/Button.jsx';
import FormField from '../components/form/FormField.jsx';
import PasswordField from '../components/form/PasswordField.jsx';

import { useAuthStore } from '../store/authStore.js';
import { useInstagramStore } from '../store/instagramStore.js';
import { cn, formatRelative } from '../utils/helpers.js';
import { CONNECTION_STATUS } from '../utils/constants.js';

const TABS = [
  { id: 'profile',       label: 'Profile',                  icon: User },
  { id: 'security',      label: 'Security',                 icon: Lock },
  { id: 'instagram',     label: 'Connected Instagram',      icon: Instagram },
  { id: 'notifications', label: 'Notifications',            icon: Bell },
];

const Settings = () => {
  const [searchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const initialTab = TABS.some((t) => t.id === requested) ? requested : 'profile';
  const [tab, setTab] = useState(initialTab);
  return (
    <div>
      <PageHeader
        title="Settings"
        description="Manage your profile, security, and integrations."
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[240px_1fr] lg:gap-8">
        {/* Side nav */}
        <nav className="flex gap-1 overflow-x-auto rounded-2xl border border-ink-100 bg-white p-1.5 dark:border-ink-800 dark:bg-ink-900 lg:h-fit lg:flex-col lg:gap-0.5 lg:p-2">
          {TABS.map((t) => (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={cn(
                'flex shrink-0 items-center gap-3 whitespace-nowrap rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                tab === t.id
                  ? 'bg-ink-900 text-white dark:bg-brand-600'
                  : 'text-ink-700 hover:bg-ink-100 dark:text-ink-300 dark:hover:bg-ink-800'
              )}
            >
              <t.icon size={16} />
              {t.label}
            </button>
          ))}
        </nav>

        {/* Content */}
        <div>
          {tab === 'profile'       && <ProfileTab />}
          {tab === 'security'      && <SecurityTab />}
          {tab === 'instagram'     && <InstagramTab />}
          {tab === 'notifications' && <NotificationsTab />}
        </div>
      </div>
    </div>
  );
};

// ─── Profile ─────────────────────────────────────────
const ProfileTab = () => {
  const user = useAuthStore((s) => s.user);
  const refreshUser = useAuthStore((s) => s.refreshUser);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      name:  user?.name  || '',
      email: user?.email || '',
    },
  });

  const onSubmit = async ({ current, next }) => {
    try {
      await authService.changePassword({ currentPassword: current, newPassword: next });
      toast.success('Password updated.');
      reset({ current: '', next: '', confirm: '' });
    } catch (e) {
      toast.error(
        e?.response?.data?.message ||
          'Could not update password. Check your current password.'
      );
    }
  };

  return (
    <Card>
      <CardHeader
        title="Profile"
        description="Update your personal information."
      />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField
          label="Full name"
          leftIcon={User}
          error={errors.name?.message}
          {...register('name', { required: 'Name is required' })}
        />
        <FormField
          label="Email"
          type="email"
          leftIcon={Mail}
          disabled
          hint="Contact support to change your email."
          {...register('email')}
        />
        <div className="pt-2">
          <Button type="submit" isLoading={isSubmitting}>
            Save changes
          </Button>
        </div>
      </form>
    </Card>
  );
};

// ─── Security ────────────────────────────────────────
const SecurityTab = () => {
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: { current: '', next: '', confirm: '' },
  });
  const newPassword = watch('next');

  const onSubmit = async () => {
    await new Promise((r) => setTimeout(r, 500));
    toast.success('Password updated.');
    reset({ current: '', next: '', confirm: '' });
  };

  return (
    <Card>
      <CardHeader
        title="Password"
        description="Change your password regularly to keep your account safe."
      />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <PasswordField
          label="Current password"
          leftIcon={KeyRound}
          error={errors.current?.message}
          {...register('current', { required: 'Current password is required' })}
        />
        <PasswordField
          label="New password"
          error={errors.next?.message}
          {...register('next', {
            required: 'New password is required',
            minLength: { value: 8, message: 'At least 8 characters' },
          })}
        />
        <PasswordField
          label="Confirm new password"
          error={errors.confirm?.message}
          {...register('confirm', {
            required: 'Please confirm your new password',
            validate: (v) => v === newPassword || 'Passwords do not match',
          })}
        />
        <div className="pt-2">
          <Button type="submit" isLoading={isSubmitting}>
            Update password
          </Button>
        </div>
      </form>
    </Card>
  );
};

// ─── Instagram (functional) ──────────────────────────
const STATUS_META = {
  [CONNECTION_STATUS.CONNECTED]:     { tone: 'success', label: 'Connected' },
  [CONNECTION_STATUS.EXPIRED]:       { tone: 'warning', label: 'Token expired' },
  [CONNECTION_STATUS.NOT_CONNECTED]: { tone: 'neutral', label: 'Not connected' },
};

const InstagramTab = () => {
  const status         = useInstagramStore((s) => s.status);
  const account        = useInstagramStore((s) => s.account);
  const isLoading      = useInstagramStore((s) => s.isLoading);
  const isConnecting   = useInstagramStore((s) => s.isConnecting);
  const fetchStatus    = useInstagramStore((s) => s.fetchStatus);
  const startConnect   = useInstagramStore((s) => s.startConnect);
  const disconnect     = useInstagramStore((s) => s.disconnect);

  useEffect(() => { fetchStatus(); }, [fetchStatus]);

  const meta = STATUS_META[status] || STATUS_META[CONNECTION_STATUS.NOT_CONNECTED];

  const handleConnect = async () => {
    try {
      // startConnect performs a hard browser redirect on success;
      // we only land here if the /connect call itself failed.
      await startConnect();
    } catch {
      toast.error('Could not start the Instagram connection.');
    }
  };

  const handleDisconnect = async () => {
    if (!window.confirm('Disconnect your Instagram account from CreatorEngine?')) return;
    try {
      await disconnect();
      toast.success('Instagram account disconnected.');
    } catch {
      toast.error('Failed to disconnect.');
    }
  };

  const handleRefresh = () => fetchStatus();

  // Pick the right body for the current state
  let body;
  if (isLoading && !account) {
    body = (
      <div className="flex items-center justify-center gap-2 py-12 text-sm text-ink-500 dark:text-ink-400">
        <Loader2 size={16} className="animate-spin" />
        Loading connection status…
      </div>
    );
  } else if (status === CONNECTION_STATUS.CONNECTED || status === CONNECTION_STATUS.EXPIRED) {
    body = (
      <ConnectedCard
        account={account}
        expired={status === CONNECTION_STATUS.EXPIRED}
        onDisconnect={handleDisconnect}
        onReconnect={handleConnect}
        isConnecting={isConnecting}
      />
    );
  } else {
    body = <NotConnectedCard onConnect={handleConnect} isConnecting={isConnecting} />;
  }

  return (
    <Card>
      <CardHeader
        title="Connected Instagram account"
        description="Link your Instagram Business account to receive comments, DMs, and story replies."
        action={
          <div className="flex items-center gap-2">
            <Badge tone={meta.tone} dot>{meta.label}</Badge>
            <button
              type="button"
              onClick={handleRefresh}
              className="rounded-lg p-1.5 text-ink-500 transition-colors hover:bg-ink-100 hover:text-ink-700 dark:hover:bg-ink-800 dark:hover:text-ink-200"
              title="Refresh status"
              aria-label="Refresh status"
              disabled={isLoading}
            >
              <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
            </button>
          </div>
        }
      />
      {body}
    </Card>
  );
};

// ─── Account details when connected (or expired) ─────
const ConnectedCard = ({ account, expired, onDisconnect, onReconnect, isConnecting }) => (
  <div className="space-y-5">
    {expired && (
      <div className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-500/30 dark:bg-amber-500/10">
        <AlertTriangle size={18} className="mt-0.5 shrink-0 text-amber-600 dark:text-amber-400" />
        <div className="min-w-0 flex-1 text-sm">
          <p className="font-semibold text-amber-900 dark:text-amber-200">
            Your Instagram access token has expired
          </p>
          <p className="mt-0.5 text-amber-800 dark:text-amber-300/90">
            Reconnect to keep receiving webhook events.
          </p>
        </div>
        <Button
          size="sm"
          onClick={onReconnect}
          isLoading={isConnecting}
          leftIcon={Instagram}
        >
          Reconnect
        </Button>
      </div>
    )}

    <div className="flex flex-col items-start gap-5 rounded-2xl border border-ink-100 bg-white p-5 dark:border-ink-800 dark:bg-ink-900 sm:flex-row sm:items-center">
      {account?.profilePictureUrl ? (
        <img
          src={account.profilePictureUrl}
          alt={account.username}
          className="h-14 w-14 shrink-0 rounded-2xl object-cover"
        />
      ) : (
        <span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-gradient-to-br from-pink-500 via-fuchsia-500 to-amber-400 text-white shadow-soft">
          <Instagram size={26} />
        </span>
      )}

      <div className="min-w-0 flex-1">
        <p className="text-base font-semibold text-ink-900 dark:text-ink-100">
          {account?.name || account?.username || 'Instagram account'}
        </p>
        <p className="text-sm text-ink-500 dark:text-ink-400">
          @{account?.username || '—'}
        </p>
      </div>

      <Button
        variant="secondary"
        leftIcon={Link2Off}
        onClick={onDisconnect}
        className="sm:shrink-0"
      >
        Disconnect
      </Button>
    </div>

    {/* Metadata grid */}
    <dl className="grid grid-cols-1 gap-4 rounded-2xl border border-ink-100 bg-white p-5 text-sm dark:border-ink-800 dark:bg-ink-900 sm:grid-cols-3">
      <Meta label="Instagram user ID" value={account?.instagramUserId || '—'} mono />
      <Meta label="Page ID"            value={account?.pageId || '—'} mono />
      <Meta label="Connected"          value={account?.connectedAt ? formatRelative(account.connectedAt) : '—'} />
      <Meta label="Last sync"          value={account?.lastSyncAt ? formatRelative(account.lastSyncAt) : 'No events yet'} />
      <Meta label="Token expires"      value={account?.tokenExpiresAt ? formatRelative(account.tokenExpiresAt) : '—'} />
    </dl>
  </div>
);

const Meta = ({ label, value, mono = false }) => (
  <div>
    <dt className="text-xs uppercase tracking-wider text-ink-400 dark:text-ink-500">
      {label}
    </dt>
    <dd
      className={`mt-0.5 truncate text-ink-800 dark:text-ink-200 ${
        mono ? 'font-mono text-xs' : ''
      }`}
      title={value}
    >
      {value}
    </dd>
  </div>
);

// ─── Empty state ─────────────────────────────────────
const NotConnectedCard = ({ onConnect, isConnecting }) => (
  <div className="flex flex-col items-start gap-5 rounded-2xl border border-dashed border-ink-200 bg-ink-50/40 p-6 dark:border-ink-800 dark:bg-ink-800/20 sm:flex-row sm:items-center">
    <span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-gradient-to-br from-pink-500 via-fuchsia-500 to-amber-400 text-white shadow-soft">
      <Instagram size={26} />
    </span>
    <div className="min-w-0 flex-1">
      <h4 className="text-base font-semibold text-ink-900 dark:text-ink-100">
        Connect your Instagram Business account
      </h4>
      <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
        We&apos;ll redirect you to Meta to authorize CreatorEngine. After
        you return, comments, DMs, and story replies start flowing in
        automatically.
      </p>
    </div>
    <Button
      leftIcon={Instagram}
      onClick={onConnect}
      isLoading={isConnecting}
      className="sm:shrink-0"
    >
      Connect Instagram
    </Button>
  </div>
);

// ─── Notifications ───────────────────────────────────
const NOTIFICATION_PREFS = [
  {
    key: 'weekly_digest',
    title: 'Weekly digest',
    description: 'A summary of your automations’ performance every Monday.',
    default: true,
  },
  {
    key: 'new_conversation',
    title: 'New conversations',
    description: 'Email me when a new DM thread starts from an automation.',
    default: true,
  },
  {
    key: 'automation_errors',
    title: 'Automation errors',
    description: 'Get notified when an automation fails or is paused by the system.',
    default: true,
  },
  {
    key: 'product_updates',
    title: 'Product updates',
    description: 'Occasional emails about new features and improvements.',
    default: false,
  },
];

const NotificationsTab = () => {
  const [prefs, setPrefs] = useState(() =>
    Object.fromEntries(NOTIFICATION_PREFS.map((p) => [p.key, p.default]))
  );

  const toggle = (key) => {
    setPrefs((p) => {
      const next = { ...p, [key]: !p[key] };
      toast.success(`${NOTIFICATION_PREFS.find((x) => x.key === key).title} ${next[key] ? 'enabled' : 'disabled'}`);
      return next;
    });
  };

  return (
    <Card>
      <CardHeader
        title="Email notifications"
        description="Choose what we email you about."
      />
      <ul className="divide-y divide-ink-100 dark:divide-ink-800">
        {NOTIFICATION_PREFS.map((p) => (
          <li
            key={p.key}
            className="flex items-start justify-between gap-4 py-4 first:pt-0 last:pb-0"
          >
            <div className="min-w-0">
              <p className="text-sm font-medium text-ink-900 dark:text-ink-100">
                {p.title}
              </p>
              <p className="mt-0.5 text-sm text-ink-500 dark:text-ink-400">
                {p.description}
              </p>
            </div>
            <Switch
              checked={prefs[p.key]}
              onChange={() => toggle(p.key)}
              srLabel={p.title}
            />
          </li>
        ))}
      </ul>
    </Card>
  );
};

export default Settings;
