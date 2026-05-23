import { Edit3, MessageCircle, Link2, Hourglass, UserCheck } from 'lucide-react';
import FormField from '../form/FormField.jsx';
import Switch from '../ui/Switch.jsx';
import Badge from '../ui/Badge.jsx';
import DmPreview from './DmPreview.jsx';
import IconButton from '../ui/IconButton.jsx';
import { useBuilderStore } from '../../store/builderStore.js';
import {
  TRIGGER_LABEL,
  ACTION_LABEL,
  CONDITION_TYPE,
  ACTION_TYPE,
  MATCH_TYPE,
} from '../../utils/constants.js';
import { renderTemplate } from '../../utils/automationEngine.js';

const MATCH_LABEL = {
  [MATCH_TYPE.CONTAINS]: 'contains',
  [MATCH_TYPE.EXACT]:    'is exactly',
};

const ICON_FOR_TYPE = {
  [ACTION_TYPE.SEND_DM]:      MessageCircle,
  [ACTION_TYPE.SEND_MESSAGE]: MessageCircle,
  [ACTION_TYPE.SEND_LINK]:    Link2,
  [ACTION_TYPE.SAVE_CONTACT]: UserCheck,
  [ACTION_TYPE.DELAY]:        Hourglass,
};

const Row = ({ label, value, onEdit }) => (
  <div className="flex items-start justify-between gap-4 border-b border-ink-100 py-3 last:border-0 dark:border-ink-800">
    <div className="min-w-0 flex-1">
      <p className="text-xs uppercase tracking-wider text-ink-400 dark:text-ink-500">
        {label}
      </p>
      <div className="mt-1 text-sm font-medium text-ink-900 dark:text-ink-100">
        {value}
      </div>
    </div>
    {onEdit && (
      <IconButton size="sm" onClick={onEdit} aria-label={`Edit ${label}`}>
        <Edit3 size={13} />
      </IconButton>
    )}
  </div>
);

const ReviewStep = () => {
  const draft     = useBuilderStore((s) => s.draft);
  const setName   = useBuilderStore((s) => s.setName);
  const setEnabled= useBuilderStore((s) => s.setEnabled);
  const goToStep  = useBuilderStore((s) => s.goToStep);

  const isKeyword = draft.condition?.type === CONDITION_TYPE.KEYWORD;

  // The chain is the canonical truth. Fall back to wrapping legacy
  // draft.action + draft.message for any old draft state that might
  // not have been migrated.
  const chain = Array.isArray(draft.actions) && draft.actions.length > 0
    ? draft.actions
    : (draft.action
        ? [{ type: draft.action.type, message: draft.message, link: draft.action.link }]
        : []);

  // Preview pane: first send-type action (chain may start with DELAY).
  const previewAction = chain.find(
    (a) => a && (a.type === ACTION_TYPE.SEND_MESSAGE
              || a.type === ACTION_TYPE.SEND_DM
              || a.type === ACTION_TYPE.SEND_LINK)
  );
  const previewRendered = previewAction
    ? renderTemplate(previewAction.message, { username: 'aria.patel' })
    : '';
  const previewLink = previewAction?.type === ACTION_TYPE.SEND_LINK
    ? previewAction.link
    : null;

  return (
    <div>
      <header className="mb-6">
        <h2 className="text-xl font-semibold text-ink-900 dark:text-ink-100">
          Review &amp; save
        </h2>
        <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
          Double-check the configuration. You can edit any step from here.
        </p>
      </header>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-5">
          <FormField
            label="Name (optional)"
            placeholder="e.g. Link drop on giveaway reel"
            value={draft.name}
            onChange={(e) => setName(e.target.value)}
            hint="If left blank we'll auto-name it from the trigger and first action."
          />

          <div className="rounded-2xl border border-ink-100 bg-white p-4 dark:border-ink-800 dark:bg-ink-900">
            <Row
              label="Trigger"
              value={TRIGGER_LABEL[draft.trigger] || '—'}
              onEdit={() => goToStep(1)}
            />
            <Row
              label="Condition"
              value={
                isKeyword
                  ? `Keyword ${MATCH_LABEL[draft.condition.matchType] || 'matches'} "${draft.condition.keyword || ''}"`
                  : 'Any incoming event'
              }
              onEdit={() => goToStep(2)}
            />
            <Row
              label={chain.length === 1 ? 'Step' : `Steps (${chain.length})`}
              value={<ChainSummary chain={chain} />}
              onEdit={() => goToStep(3)}
            />
          </div>

          {/* Enable on save */}
          <div className="flex items-center justify-between rounded-2xl border border-ink-100 bg-white p-4 dark:border-ink-800 dark:bg-ink-900">
            <div>
              <p className="text-sm font-semibold text-ink-900 dark:text-ink-100">
                Enable on save
              </p>
              <p className="text-xs text-ink-500 dark:text-ink-400">
                {draft.enabled
                  ? 'This automation will start firing immediately once saved.'
                  : 'Saved as paused — turn it on later from the list.'}
              </p>
            </div>
            <Switch
              checked={draft.enabled}
              onChange={setEnabled}
              srLabel="Enable on save"
            />
          </div>
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <p className="label mb-0">Preview</p>
            <Badge tone="brand" dot>Live</Badge>
          </div>
          {previewAction ? (
            <DmPreview
              message={previewRendered}
              recipientHandle="aria.patel"
              link={previewLink}
            />
          ) : (
            <p className="rounded-2xl border border-dashed border-ink-200 bg-white px-4 py-6 text-center text-sm text-ink-500 dark:border-ink-700 dark:bg-ink-900 dark:text-ink-400">
              No message to preview — this chain only saves contacts or pauses.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

// ─── Compact per-action summary line ───────────────
const ChainSummary = ({ chain }) => {
  if (!chain.length) {
    return <em className="text-ink-400">No steps configured</em>;
  }
  return (
    <ol className="space-y-1.5">
      {chain.map((action, i) => {
        const Icon = ICON_FOR_TYPE[action?.type] || MessageCircle;
        return (
          <li
            key={i}
            className="flex items-start gap-2 text-sm font-normal text-ink-700 dark:text-ink-300"
          >
            <span className="mt-0.5 grid h-5 w-5 shrink-0 place-items-center rounded bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300">
              <Icon size={12} />
            </span>
            <span className="min-w-0 flex-1">
              <span className="font-medium text-ink-900 dark:text-ink-100">
                {ACTION_LABEL[action?.type] || action?.type || 'Step'}
              </span>
              <ActionDetail action={action} />
            </span>
          </li>
        );
      })}
    </ol>
  );
};

// ─── Tail text for each action ─────────────────────
const ActionDetail = ({ action }) => {
  if (!action) return null;

  if (action.type === ACTION_TYPE.DELAY) {
    const secs = Number(action.delaySeconds) || 0;
    return (
      <span className="text-ink-500 dark:text-ink-400">
        {' '}— {humanDuration(secs)}
      </span>
    );
  }

  if (action.type === ACTION_TYPE.SAVE_CONTACT) {
    return null;
  }

  if (action.type === ACTION_TYPE.SEND_LINK) {
    return (
      <>
        {action.message && (
          <span className="block truncate text-xs text-ink-500 dark:text-ink-400">
            “{action.message}”
          </span>
        )}
        {action.link && (
          <span className="block truncate text-xs text-ink-500 dark:text-ink-400">
            → {action.link}
          </span>
        )}
      </>
    );
  }

  // SEND_MESSAGE / SEND_DM
  if (action.message) {
    return (
      <span className="block truncate text-xs text-ink-500 dark:text-ink-400">
        “{action.message}”
      </span>
    );
  }
  return (
    <span className="block text-xs italic text-ink-400">No message</span>
  );
};

const humanDuration = (totalSecs) => {
  if (totalSecs <= 0) return 'a moment';
  if (totalSecs < 60) return `${totalSecs} second${totalSecs === 1 ? '' : 's'}`;
  const mins = Math.round(totalSecs / 60);
  if (mins < 60) return `${mins} minute${mins === 1 ? '' : 's'}`;
  const hours = Math.round(mins / 60);
  return `${hours} hour${hours === 1 ? '' : 's'}`;
};

export default ReviewStep;
