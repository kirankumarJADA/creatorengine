import { UserPlus } from 'lucide-react';
import { useBuilderStore } from '../../store/builderStore.js';
import { cn } from '../../utils/helpers.js';

const MAX_MESSAGE = 1000;
const MAX_BUTTON = 20;
const DEFAULT_MESSAGE =
  "Hey {{username}}! 🙌 Make sure you're following me, then tap the button below and I'll send it right over 👇";
const DEFAULT_BUTTON = 'I Followed ✅';

/**
 * Follow-gate settings — ask the commenter to follow before delivering the DM
 * content. Trust-based: they tap the button, content is sent. Renders only for
 * comment-triggered automations.
 */
const FollowGateEditor = () => {
  const trigger     = useBuilderStore((s) => s.draft.trigger);
  const enabled     = useBuilderStore((s) => s.draft.followGateEnabled);
  const message     = useBuilderStore((s) => s.draft.followGateMessage) || '';
  const buttonLabel = useBuilderStore((s) => s.draft.followGateButtonLabel) || '';
  const setEnabled     = useBuilderStore((s) => s.setFollowGateEnabled);
  const setMessage     = useBuilderStore((s) => s.setFollowGateMessage);
  const setButtonLabel = useBuilderStore((s) => s.setFollowGateButtonLabel);

  const isCommentTrigger = String(trigger || '').toUpperCase().includes('COMMENT');
  if (!isCommentTrigger) return null;

  const handleToggle = () => {
    if (!enabled) {
      if (!message.trim()) setMessage(DEFAULT_MESSAGE);
      if (!buttonLabel.trim()) setButtonLabel(DEFAULT_BUTTON);
    }
    setEnabled(!enabled);
  };

  return (
    <section className="mt-8 border-t border-ink-100 pt-6 dark:border-ink-800">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="flex items-center gap-2 text-base font-semibold text-ink-900 dark:text-ink-100">
            <UserPlus size={16} className="text-brand-600 dark:text-brand-400" />
            Ask to follow first
          </h3>
          <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
            Before delivering your content, send a DM asking the commenter to follow you.
            They tap a button to confirm, then the content is sent — trust-based, no follower checking.
          </p>
        </div>
        <Switch on={enabled} onClick={handleToggle} />
      </div>

      {enabled && (
        <div className="mt-4 space-y-4 rounded-xl border border-ink-100 p-4 dark:border-ink-800">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink-700 dark:text-ink-200">
              Follow message
            </label>
            <textarea
              rows={3}
              maxLength={MAX_MESSAGE}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Hey {{username}}! Follow me and tap the button to get it 👇"
              className="input w-full resize-none text-sm"
            />
            <div className="mt-1 flex items-center justify-between text-xs text-ink-400 dark:text-ink-500">
              <span>Use <code className="font-mono">{`{{username}}`}</code> to mention them.</span>
              <span>{message.length} / {MAX_MESSAGE}</span>
            </div>
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink-700 dark:text-ink-200">
              Button label
            </label>
            <input
              type="text"
              maxLength={MAX_BUTTON}
              value={buttonLabel}
              onChange={(e) => setButtonLabel(e.target.value)}
              placeholder="I Followed ✅"
              className="input w-full text-sm"
            />
            <p className="mt-1 text-right text-xs text-ink-400 dark:text-ink-500">
              {buttonLabel.length} / {MAX_BUTTON}
            </p>
          </div>
        </div>
      )}
    </section>
  );
};

const Switch = ({ on, onClick }) => (
  <button
    type="button"
    role="switch"
    aria-checked={on}
    onClick={onClick}
    className={cn(
      'relative h-6 w-11 shrink-0 rounded-full transition-colors',
      on ? 'bg-brand-600' : 'bg-ink-200 dark:bg-ink-700'
    )}
  >
    <span
      className={cn(
        'absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform',
        on ? 'translate-x-5' : 'translate-x-0'
      )}
    />
  </button>
);

export default FollowGateEditor;