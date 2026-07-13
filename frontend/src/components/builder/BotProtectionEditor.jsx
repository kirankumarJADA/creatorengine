import { Shield, Info } from 'lucide-react';
import { useBuilderStore } from '../../store/builderStore.js';
import { cn } from '../../utils/helpers.js';

/**
 * Bot Protection settings panel shown in the automation builder.
 * Lets users enable random timing jitter to reduce Instagram's ability
 * to detect repetitive/automated sending patterns.
 *
 * Covers:
 *  - Toggle: enable/disable bot protection
 *  - Min/Max delay seconds for per-action jitter
 *  - Message Variations (already on each action) are mentioned here
 *    as part of the same protection feature
 */
const BotProtectionEditor = () => {
  const draft = useBuilderStore((s) => s.draft);
  const setBotProtectionEnabled = useBuilderStore((s) => s.setBotProtectionEnabled);
  const setBotProtectionMinDelay = useBuilderStore((s) => s.setBotProtectionMinDelay);
  const setBotProtectionMaxDelay = useBuilderStore((s) => s.setBotProtectionMaxDelay);

  const enabled = draft.botProtectionEnabled ?? false;
  const minDelay = draft.botProtectionMinDelaySeconds ?? 2;
  const maxDelay = draft.botProtectionMaxDelaySeconds ?? 8;

  return (
    <div className="mt-6 rounded-2xl border border-ink-100 bg-white p-5 dark:border-ink-800 dark:bg-ink-900">
      {/* Header row */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-400">
            <Shield size={18} />
          </span>
          <div>
            <p className="font-semibold text-ink-900 dark:text-ink-100">Bot Protection</p>
            <p className="text-sm text-ink-500 dark:text-ink-400">
                 Premium Bot Protection uses intelligent message rotation and natural timing to create authentic conversations while preserving your automation flow.i changed it         </p>
          </div>
        </div>

        {/* Toggle */}
        <button
          type="button"
          role="switch"
          aria-checked={enabled}
          onClick={() => setBotProtectionEnabled(!enabled)}
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

          {/* Jitter delay range */}
          <div>
            <p className="mb-2 text-sm font-medium text-ink-800 dark:text-ink-200">
              Random delay before each send
            </p>
            <div className="flex items-center gap-3">
              <div className="flex flex-col gap-1">
                <label className="text-xs text-ink-500 dark:text-ink-400">Min (seconds)</label>
                <input
                  type="number"
                  min={0}
                  max={maxDelay}
                  value={minDelay}
                  onChange={(e) => {
                    const v = Math.max(0, Math.min(Number(e.target.value), maxDelay));
                    setBotProtectionMinDelay(v);
                  }}
                  className="input w-24"
                />
              </div>
              <span className="mt-4 text-ink-400">–</span>
              <div className="flex flex-col gap-1">
                <label className="text-xs text-ink-500 dark:text-ink-400">Max (seconds)</label>
                <input
                  type="number"
                  min={minDelay}
                  max={60}
                  value={maxDelay}
                  onChange={(e) => {
                    const v = Math.max(minDelay, Math.min(Number(e.target.value), 60));
                    setBotProtectionMaxDelay(v);
                  }}
                  className="input w-24"
                />
              </div>
            </div>
            <p className="mt-1.5 text-xs text-ink-500 dark:text-ink-400">
              Each DM will be sent after a random pause in this range, making the timing look human.
            </p>
          </div>

          {/* Message Variations reminder */}
          <div className="flex items-start gap-2 rounded-xl bg-ink-50 px-3 py-2.5 dark:bg-ink-800/40">
            <Info size={14} className="mt-0.5 shrink-0 text-ink-400" />
            <p className="text-xs text-ink-600 dark:text-ink-300">
              <strong>Tip:</strong> Add message variations on each step (using the Variations button) to
              rotate between different texts — prevents Instagram flagging identical repeated messages.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default BotProtectionEditor;