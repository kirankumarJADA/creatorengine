import { Card } from '../ui/Card.jsx';
import { Instagram, KeyRound, Send, Radio, Zap, Activity } from 'lucide-react';
import { cn } from '../../utils/helpers.js';

const timeAgo = (iso) => {
  if (!iso) return null;
  const diff = Date.now() - new Date(iso).getTime();
  const sec = Math.max(0, Math.floor(diff / 1000));
  if (sec < 60) return `${sec}s ago`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}m ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}h ago`;
  return `${Math.floor(hr / 24)}d ago`;
};

const TONE = {
  green:   'bg-emerald-500',
  amber:   'bg-amber-500',
  red:     'bg-rose-500',
  neutral: 'bg-ink-300 dark:bg-ink-600',
};

/**
 * Compact live health strip for the dashboard. All values derive from
 * data the dashboard already fetches (IG status, logs, automations) —
 * no extra backend calls.
 */
const SystemStatus = ({ igStatus, logs = [], automations = [] }) => {
  const igConnected = Boolean(igStatus?.username);

  // Token health
  const expiresAt    = igStatus?.tokenExpiresAt ? new Date(igStatus.tokenExpiresAt).getTime() : null;
  const daysToExpiry = expiresAt != null ? Math.floor((expiresAt - Date.now()) / 86400000) : null;
  let tokenTone = 'neutral';
  let tokenText = '—';
  if (igConnected && daysToExpiry != null) {
    if (daysToExpiry > 7)       { tokenTone = 'green'; tokenText = `${daysToExpiry}d left`; }
    else if (daysToExpiry >= 0) { tokenTone = 'amber'; tokenText = `${daysToExpiry}d left`; }
    else                        { tokenTone = 'red';   tokenText = 'Expired'; }
  } else if (igConnected) {
    tokenTone = 'green';
    tokenText = 'Valid';
  }

  const lastDm       = logs.find((l) => l.messageSent);
  const lastDmAgo    = lastDm ? timeAgo(lastDm.timestamp) : null;
  const lastEventAgo = logs[0] ? timeAgo(logs[0].timestamp) : null;
  const activeCount  = automations.filter((a) => a.enabled).length;
  const healthy      = igConnected && (daysToExpiry == null || daysToExpiry >= 0);

  const items = [
    { icon: Instagram, label: 'Instagram',   tone: igConnected ? 'green' : 'red',
      value: igConnected ? `@${igStatus.username}` : 'Not connected' },
    { icon: KeyRound,  label: 'Token',        tone: tokenTone, value: tokenText },
    { icon: Radio,     label: 'Last event',   tone: lastEventAgo ? 'green' : 'neutral',
      value: lastEventAgo || 'None yet' },
    { icon: Send,      label: 'Last DM sent',  tone: lastDmAgo ? 'green' : 'neutral',
      value: lastDmAgo || 'None yet' },
    { icon: Zap,       label: 'Active autos',  tone: activeCount > 0 ? 'green' : 'amber',
      value: String(activeCount) },
    { icon: Activity,  label: 'System',        tone: healthy ? 'green' : 'amber',
      value: healthy ? 'Healthy' : 'Check' },
  ];

  return (
    <Card className="mb-6">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-ink-900 dark:text-ink-100">System status</h3>
        <span className="text-xs text-ink-400 dark:text-ink-500">live</span>
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        {items.map((it) => (
          <div
            key={it.label}
            className="flex items-center gap-2.5 rounded-xl border border-ink-100 p-2.5 dark:border-ink-800"
          >
            <span className="relative grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-ink-50 text-ink-600 dark:bg-ink-800 dark:text-ink-300">
              <it.icon size={15} />
              <span className={cn('absolute -right-0.5 -top-0.5 h-2.5 w-2.5 rounded-full ring-2 ring-white dark:ring-ink-900', TONE[it.tone])} />
            </span>
            <div className="min-w-0">
              <p className="truncate text-[11px] uppercase tracking-wide text-ink-400 dark:text-ink-500">{it.label}</p>
              <p className="truncate text-sm font-medium text-ink-900 dark:text-ink-100">{it.value}</p>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
};

export default SystemStatus;