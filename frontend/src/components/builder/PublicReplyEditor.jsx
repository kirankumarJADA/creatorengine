import { Megaphone, Plus, Trash2 } from 'lucide-react';
import { useBuilderStore } from '../../store/builderStore.js';
import { TRIGGER_TYPE } from '../../utils/constants.js';
import { cn } from '../../utils/helpers.js';

const MAX_REPLIES = 10;
const MAX_LENGTH = 300;

const DEFAULT_TEMPLATES = [
  'Sent you a DM! 📩',
  'Check your inbox 📬',
  'Just messaged you — check it out!',
];

/** Triggers that operate on comments. */
const COMMENT_LIKE_TRIGGERS = new Set([TRIGGER_TYPE.COMMENT, TRIGGER_TYPE.NEXT_POST]);

const PublicReplyEditor = () => {
  const trigger              = useBuilderStore((s) => s.draft.trigger);
  const enabled              = useBuilderStore((s) => s.draft.publicReplyEnabled);
  const replies              = useBuilderStore((s) => s.draft.publicReplies) || [];
  const setPublicReplyEnabled = useBuilderStore((s) => s.setPublicReplyEnabled);
  const setPublicReplies      = useBuilderStore((s) => s.setPublicReplies);
  const addPublicReply        = useBuilderStore((s) => s.addPublicReply);
  const updatePublicReply     = useBuilderStore((s) => s.updatePublicReply);
  const removePublicReply     = useBuilderStore((s) => s.removePublicReply);

  if (!COMMENT_LIKE_TRIGGERS.has(trigger)) return null;

  const activeCount = replies.filter(
    (r) => r.enabled !== false && r.text && r.text.trim()
  ).length;

  const handleMasterToggle = () => {
    if (!enabled && replies.length === 0) {
      setPublicReplies(DEFAULT_TEMPLATES.map((text) => ({ text, enabled: true })));
    }
    setPublicReplyEnabled(!enabled);
  };

  return (
    <section className="mt-8 border-t border-ink-100 pt-6 dark:border-ink-800">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="flex items-center gap-2 text-base font-semibold text-ink-900 dark:text-ink-100">
            <Megaphone size={16} className="text-brand-600 dark:text-brand-400" />
            Publicly reply to comments
          </h3>
          <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
            Also post a short public reply on the comment, so everyone sees it.
            One active template is picked at random. Use{' '}
            <code className="font-mono text-xs">{`{{username}}`}</code> to mention the commenter.
          </p>
        </div>
        <Switch on={enabled} onClick={handleMasterToggle} />
      </div>

      {enabled && (
        <div className="mt-4 rounded-xl border border-ink-100 p-4 dark:border-ink-800">
          <div className="mb-3 flex items-center justify-between">
            <span className="text-sm font-medium text-ink-700 dark:text-ink-200">
              Public replies
            </span>
            <span className="text-xs font-medium text-brand-700 dark:text-brand-400">
              {activeCount} active
            </span>
          </div>

          <ul className="space-y-3">
            {replies.map((reply, i) => {
              const isOn = reply.enabled !== false;
              return (
                <li key={i} className="flex items-start gap-2.5">
                  <div className="pt-2">
                    <Switch
                      small
                      on={isOn}
                      onClick={() => updatePublicReply(i, { enabled: !isOn })}
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <textarea
                      rows={2}
                      maxLength={MAX_LENGTH}
                      value={reply.text || ''}
                      onChange={(e) => updatePublicReply(i, { text: e.target.value })}
                      placeholder="e.g. Sent you a DM! 📩"
                      className={cn(
                        'input w-full resize-none text-sm',
                        !isOn && 'opacity-50'
                      )}
                    />
                    <p className="mt-1 text-xs text-ink-400 dark:text-ink-500">
                      {(reply.text || '').length} / {MAX_LENGTH}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => removePublicReply(i)}
                    className="mt-2 rounded-lg p-1.5 text-ink-400 transition-colors hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-500/10 dark:hover:text-red-400"
                    aria-label="Remove reply"
                  >
                    <Trash2 size={15} />
                  </button>
                </li>
              );
            })}
          </ul>

          {replies.length < MAX_REPLIES && (
            <button
              type="button"
              onClick={() => addPublicReply('')}
              className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl border border-dashed border-ink-200 py-2.5 text-sm font-medium text-ink-600 transition-colors hover:border-brand-400 hover:text-brand-700 dark:border-ink-700 dark:text-ink-300 dark:hover:border-brand-500 dark:hover:text-brand-400"
            >
              <Plus size={15} />
              Add public reply
            </button>
          )}
        </div>
      )}
    </section>
  );
};

const Switch = ({ on, onClick, small = false }) => (
  <button
    type="button"
    role="switch"
    aria-checked={on}
    onClick={onClick}
    className={cn(
      'relative shrink-0 rounded-full transition-colors',
      small ? 'h-5 w-9' : 'h-6 w-11',
      on ? 'bg-brand-600' : 'bg-ink-200 dark:bg-ink-700'
    )}
  >
    <span
      className={cn(
        'absolute left-0.5 top-0.5 rounded-full bg-white shadow transition-transform',
        small ? 'h-4 w-4' : 'h-5 w-5',
        on ? (small ? 'translate-x-4' : 'translate-x-5') : 'translate-x-0'
      )}
    />
  </button>
);

export default PublicReplyEditor;