import { Mail } from 'lucide-react';
import { useBuilderStore } from '../../store/builderStore.js';
import TextArea from '../ui/TextArea.jsx';
import { cn } from '../../utils/helpers.js';

/**
 * Email Collection
 *
 * When enabled, after the automation's DM is sent the engine watches for
 * the contact to reply with an email address. If they do, it is saved to
 * their Contact record automatically (within 48 hours of the original DM).
 *
 * Optionally the creator can set a custom "ask" message that will be sent
 * as an extra DM right after the main automation message — e.g.
 * "Drop your email below and I'll send you the full guide 👇"
 */
const EmailCollectEditor = () => {
  const draft = useBuilderStore((s) => s.draft);
  const setEmailCollectEnabled = useBuilderStore((s) => s.setEmailCollectEnabled);
  const setEmailCollectMessage = useBuilderStore((s) => s.setEmailCollectMessage);

  const enabled = draft.emailCollectEnabled ?? false;
  const askMessage = draft.emailCollectMessage ?? '';

  return (
    <div className="mt-6 rounded-2xl border border-ink-100 bg-white p-5 dark:border-ink-800 dark:bg-ink-900">
      {/* Header row */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600 dark:bg-brand-500/10 dark:text-brand-400">
            <Mail size={18} />
          </span>
          <div>
            <p className="font-semibold text-ink-900 dark:text-ink-100">Collect Email</p>
            <p className="text-sm text-ink-500 dark:text-ink-400">
              Save the contact's email when they reply with it
            </p>
          </div>
        </div>

        {/* Toggle */}
        <button
          type="button"
          role="switch"
          aria-checked={enabled}
          onClick={() => setEmailCollectEnabled(!enabled)}
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
          <div>
            <p className="mb-2 text-sm font-medium text-ink-800 dark:text-ink-200">
              Ask message <span className="font-normal text-ink-400">(optional)</span>
            </p>
            <TextArea
              value={askMessage}
              onChange={(e) => setEmailCollectMessage(e.target.value)}
              placeholder="Drop your email below and I'll send you the full guide 👇"
              rows={3}
            />
            <p className="mt-1.5 text-xs text-ink-500 dark:text-ink-400">
              If set, this is sent as an extra DM right after your main message, prompting
              the contact to share their email. Leave blank to skip the ask — their reply
              will still be scanned for an email address either way.
            </p>
          </div>

          <div className="rounded-xl bg-ink-50 px-4 py-3 text-xs text-ink-600 dark:bg-ink-800/40 dark:text-ink-400">
            <strong className="font-medium text-ink-700 dark:text-ink-300">How it works:</strong>{' '}
            After your automation sends its DM, we watch for a reply that contains an email
            address (up to 48 hours). When we spot one, it's saved to the contact's record in
            your Contacts tab — no action needed from you.
          </div>
        </div>
      )}
    </div>
  );
};

export default EmailCollectEditor;
