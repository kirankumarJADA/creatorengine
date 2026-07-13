import { Clock3 } from 'lucide-react';
import { useBuilderStore } from '../../store/builderStore.js';
import TextArea from '../ui/TextArea.jsx';
import { cn } from '../../utils/helpers.js';

const MAX_AMOUNT = { MINUTES: 1440, HOURS: 720, DAYS: 30 };

/**
 * Single no-reply follow-up message.
 *
 * A timer starts the moment the automation's last message is sent. If the
 * contact replies before it expires, the follow-up is cancelled
 * automatically (handled server-side). If no reply arrives in time, this
 * one follow-up message is sent and the automation is done - no further
 * follow-ups are scheduled.
 */
const FollowUpEditor = () => {
  const draft = useBuilderStore((s) => s.draft);
  const setFollowUpEnabled = useBuilderStore((s) => s.setFollowUpEnabled);
  const setFollowUpDelayAmount = useBuilderStore((s) => s.setFollowUpDelayAmount);
  const setFollowUpDelayUnit = useBuilderStore((s) => s.setFollowUpDelayUnit);
  const setFollowUpMessage = useBuilderStore((s) => s.setFollowUpMessage);

  const enabled = draft.followUpEnabled ?? false;
  const amount = draft.followUpDelayAmount ?? 1;
  const unit = draft.followUpDelayUnit ?? 'HOURS';
  const message = draft.followUpMessage ?? '';

  const amountInvalid = enabled && (!amount || amount <= 0);
  const messageInvalid = enabled && !message.trim();

  const handleAmountChange = (e) => {
    const n = Math.max(0, Math.round(Number(e.target.value) || 0));
    const max = MAX_AMOUNT[unit] ?? 999;
    setFollowUpDelayAmount(Math.min(n, max));
  };

  const handleUnitChange = (e) => {
    const nextUnit = e.target.value;
    setFollowUpDelayUnit(nextUnit);
    const max = MAX_AMOUNT[nextUnit] ?? 999;
    if (amount > max) setFollowUpDelayAmount(max);
  };

  return (
    <div className="mt-6 rounded-2xl border border-ink-100 bg-white p-5 dark:border-ink-800 dark:bg-ink-900">
      {/* Header row */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-amber-50 text-amber-600 dark:bg-amber-500/10 dark:text-amber-400">
            <Clock3 size={18} />
          </span>
          <div>
            <p className="font-semibold text-ink-900 dark:text-ink-100">Follow-up Message</p>
            <p className="text-sm text-ink-500 dark:text-ink-400">
              Automatically send one follow-up if the contact doesn't reply
            </p>
          </div>
        </div>

        {/* Toggle */}
        <button
          type="button"
          role="switch"
          aria-checked={enabled}
          onClick={() => setFollowUpEnabled(!enabled)}
          className={cn(
            'relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors',
            enabled ? 'bg-emerald-500' : 'bg-ink-200 dark:bg-ink-700'
          )}
        >
          <span
            className={cn(
              'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition-transform',
              enabled ? 'translate-x-5' : 'translate-x-0'
            )}
          />
        </button>
      </div>

      {/* Expanded settings */}
      {enabled && (
        <div className="mt-4 space-y-4 border-t border-ink-100 pt-4 dark:border-ink-800">

          {/* Delay picker — same pattern as the Delay action */}
          <div>
            <p className="mb-2 text-sm font-medium text-ink-800 dark:text-ink-200">
              When should this follow-up be sent if there's no reply?
            </p>
            <div className="flex items-center gap-2">
              <span className="text-sm text-ink-500 dark:text-ink-400">After</span>
              <input
                type="number"
                min={1}
                max={MAX_AMOUNT[unit] ?? 999}
                step={1}
                className={cn('input w-24', amountInvalid && 'border-red-300 dark:border-red-500/40')}
                value={amount || ''}
                onChange={handleAmountChange}
              />
              <select
                className="input w-32"
                value={unit}
                onChange={handleUnitChange}
              >
                <option value="MINUTES">Minutes</option>
                <option value="HOURS">Hours</option>
                <option value="DAYS">Days</option>
              </select>
            </div>
            {amountInvalid && (
              <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">
                Enter a number greater than 0.
              </p>
            )}
          </div>

          {/* Follow-up message */}
          <div>
            <p className="mb-2 text-sm font-medium text-ink-800 dark:text-ink-200">
              Follow-up message
            </p>
            <TextArea
              value={message}
              onChange={(e) => setFollowUpMessage(e.target.value)}
              placeholder="Hey {{username}}, just checking in — still interested?"
              rows={3}
              className={cn(messageInvalid && 'border-red-300 dark:border-red-500/40')}
            />
            {messageInvalid && (
              <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">
                Add a follow-up message.
              </p>
            )}
            <p className="mt-1.5 text-xs text-ink-500 dark:text-ink-400">
              Sent once, only if the contact hasn't replied by the time above. If they reply
              sooner, this is cancelled automatically.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default FollowUpEditor;