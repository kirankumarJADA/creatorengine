import { useState } from 'react';
import { Hourglass, UserCheck, MessageCircle, Link2, ArrowDown } from 'lucide-react';
import DmPreview from './DmPreview.jsx';
import PublicReplyEditor from './PublicReplyEditor.jsx';
import FollowGateEditor from './FollowGateEditor.jsx';
import BotProtectionEditor from './BotProtectionEditor.jsx';
import FollowUpEditor from './FollowUpEditor.jsx';
import { useBuilderStore } from '../../store/builderStore.js';
import { renderTemplate } from '../../utils/automationEngine.js';
import { ACTION_TYPE } from '../../utils/constants.js';
import { cn } from '../../utils/helpers.js';

const MessageStep = () => {
  const actions = useBuilderStore((s) => s.draft.actions);
  const [previewName, setPreviewName] = useState('aria.patel');

  return (
    <div>
      <header className="mb-6">
        <h2 className="text-xl font-semibold text-ink-900 dark:text-ink-100">
          How will this play out?
        </h2>
        <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
          See what the recipient will experience, step by step.
        </p>
      </header>

      {/* Username preview control */}
      <div className="mb-5 flex flex-wrap items-center gap-2 rounded-xl bg-ink-50 px-3 py-2.5 text-sm dark:bg-ink-800/40">
        <span className="text-ink-600 dark:text-ink-300">Preview as</span>
        <input
          type="text"
          value={previewName}
          onChange={(e) => setPreviewName(e.target.value)}
          placeholder="username"
          className="input h-8 max-w-[180px] py-1 text-sm"
        />
        <span className="text-ink-500 dark:text-ink-400">
          — values replace <code className="font-mono text-xs">{`{{username}}`}</code> below.
        </span>
      </div>

      {/* Step-by-step preview */}
      <ol className="space-y-3">
        {actions.map((action, i) => (
          <li key={i} className="flex flex-col gap-2">
            <StepPreview
              index={i}
              action={action}
              previewName={previewName}
            />
            {i < actions.length - 1 && (
              <span className="flex items-center justify-center text-ink-300 dark:text-ink-600">
                <ArrowDown size={16} />
              </span>
            )}
          </li>
        ))}
      </ol>

      {/* Comment-only engagement settings */}
      <PublicReplyEditor />
      <FollowGateEditor />

      {/* Bot Protection — always shown, applies to all trigger types */}
      <BotProtectionEditor />

      {/* Follow-up Message — single no-reply follow-up */}
      <FollowUpEditor />
    </div>
  );
};

// ─── One step's preview row ────────────────────────
const StepPreview = ({ index, action, previewName }) => {
  const variables = { username: previewName || 'friend' };

  if (action.type === ACTION_TYPE.SEND_MESSAGE || action.type === ACTION_TYPE.SEND_DM) {
    return (
      <PreviewShell index={index} icon={MessageCircle} label="Send message">
        {action.imageUrl && (
          <img
            src={action.imageUrl}
            alt="DM image"
            className="mb-2 h-24 w-24 rounded-lg object-cover"
          />
        )}
        <DmPreview message={renderTemplate(action.message, variables)} />
      </PreviewShell>
    );
  }

  if (action.type === ACTION_TYPE.SEND_LINK) {
    const rendered = renderTemplate(action.message, variables);
    const withLink = action.link
      ? (rendered ? `${rendered}\n${action.link}` : action.link)
      : rendered;
    return (
      <PreviewShell index={index} icon={Link2} label="Send link">
        {action.imageUrl && (
          <img
            src={action.imageUrl}
            alt="DM image"
            className="mb-2 h-24 w-24 rounded-lg object-cover"
          />
        )}
        <DmPreview message={withLink} />
      </PreviewShell>
    );
  }

  if (action.type === ACTION_TYPE.DELAY) {
    const secs = Number(action.delaySeconds) || 0;
    return (
      <PreviewShell index={index} icon={Hourglass} label="Delay">
        <p className="rounded-xl bg-amber-50 px-3 py-2.5 text-sm text-amber-800 dark:bg-amber-500/10 dark:text-amber-200">
          Wait {humanDuration(secs)} before continuing.
        </p>
      </PreviewShell>
    );
  }

  if (action.type === ACTION_TYPE.SAVE_CONTACT) {
    return (
      <PreviewShell index={index} icon={UserCheck} label="Save contact">
        <p className="rounded-xl bg-emerald-50 px-3 py-2.5 text-sm text-emerald-800 dark:bg-emerald-500/10 dark:text-emerald-200">
          Add the sender to your Contacts. Nothing is sent on Instagram.
        </p>
      </PreviewShell>
    );
  }

  return null;
};

const PreviewShell = ({ index, icon: Icon, label, children }) => (
  <div className="flex flex-col gap-2 sm:flex-row sm:items-start">
    <div className="flex shrink-0 items-center gap-2 sm:w-32">
      <span className="grid h-6 w-6 place-items-center rounded-md bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300">
        <Icon size={14} />
      </span>
      <span className="text-xs font-medium uppercase tracking-wider text-ink-500 dark:text-ink-400">
        {index + 1}. {label}
      </span>
    </div>
    <div className={cn('min-w-0 flex-1')}>{children}</div>
  </div>
);

const humanDuration = (totalSecs) => {
  if (totalSecs <= 0) return 'a moment';
  if (totalSecs < 60) return `${totalSecs} second${totalSecs === 1 ? '' : 's'}`;
  const mins = Math.round(totalSecs / 60);
  if (mins < 60) return `${mins} minute${mins === 1 ? '' : 's'}`;
  const hours = Math.round(mins / 60);
  return `${hours} hour${hours === 1 ? '' : 's'}`;
};

export default MessageStep;