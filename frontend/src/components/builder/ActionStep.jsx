import { useState } from 'react';
import {
  Send, MessageCircle, Link2, UserCheck, Hourglass,
  Plus, Trash2, Copy, ChevronUp, ChevronDown, Sparkles,
} from 'lucide-react';

import Field from '../form/Field.jsx';
import TextArea from '../ui/TextArea.jsx';
import IconButton from '../ui/IconButton.jsx';
import Button from '../form/Button.jsx';
import AiAssistantModal from './AiAssistantModal.jsx';
import { useBuilderStore } from '../../store/builderStore.js';
import {
  ACTION_TYPE,
  DELAY_MIN_SECONDS,
  DELAY_MAX_SECONDS,
} from '../../utils/constants.js';
import { cn } from '../../utils/helpers.js';

// ─── Action-type picker descriptors ────────────────
// SEND_DM is NOT shown in the new picker — it's a legacy synonym for
// SEND_MESSAGE that we keep on the entity for back-compat reads. New
// chains use SEND_MESSAGE exclusively.
const PICKER_OPTIONS = [
  { value: ACTION_TYPE.SEND_MESSAGE, label: 'Message',  icon: MessageCircle, tone: 'brand'   },
  { value: ACTION_TYPE.SEND_LINK,    label: 'Link',     icon: Link2,         tone: 'warning' },
  { value: ACTION_TYPE.DELAY,        label: 'Delay',    icon: Hourglass,     tone: 'neutral' },
  { value: ACTION_TYPE.SAVE_CONTACT, label: 'Save',     icon: UserCheck,     tone: 'success' },
];

const ICON_FOR_TYPE = {
  [ACTION_TYPE.SEND_DM]:      Send,
  [ACTION_TYPE.SEND_MESSAGE]: MessageCircle,
  [ACTION_TYPE.SEND_LINK]:    Link2,
  [ACTION_TYPE.SAVE_CONTACT]: UserCheck,
  [ACTION_TYPE.DELAY]:        Hourglass,
};

const ActionStep = ({ errors = {} }) => {
  const actions         = useBuilderStore((s) => s.draft.actions);
  const addAction       = useBuilderStore((s) => s.addAction);
  const removeAction    = useBuilderStore((s) => s.removeAction);
  const updateAction    = useBuilderStore((s) => s.updateAction);
  const duplicateAction = useBuilderStore((s) => s.duplicateAction);
  const moveAction      = useBuilderStore((s) => s.moveAction);

  // errors.actions is a list of per-action error strings (or null) when present
  const perActionErrors = Array.isArray(errors.actions) ? errors.actions : [];

  return (
    <div>
      <header className="mb-6">
        <h2 className="text-xl font-semibold text-ink-900 dark:text-ink-100">
          What should happen?
        </h2>
        <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
          Chain together one or more steps. Use <strong>Delay</strong> between
          messages to pace your replies, or end with <strong>Save</strong> to
          add the sender to your contacts.
        </p>
      </header>

      {/* Top-level chain error (e.g. "must include one non-delay action") */}
      {errors.actionsChain && (
        <p className="mb-4 text-sm text-red-600 dark:text-red-400">
          {errors.actionsChain}
        </p>
      )}

      {/* Stack of action cards */}
      <ol className="space-y-4">
        {actions.map((action, i) => (
          <ActionCard
            key={i}
            index={i}
            action={action}
            isFirst={i === 0}
            isLast={i === actions.length - 1}
            canRemove={actions.length > 1}
            error={perActionErrors[i]}
            onPatch={(patch) => updateAction(i, patch)}
            onRemove={() => removeAction(i)}
            onDuplicate={() => duplicateAction(i)}
            onMoveUp={() => moveAction(i, -1)}
            onMoveDown={() => moveAction(i, 1)}
          />
        ))}
      </ol>

      <div className="mt-5">
        <Button
          type="button"
          variant="secondary"
          leftIcon={Plus}
          onClick={() => addAction(ACTION_TYPE.SEND_MESSAGE)}
          className="w-full sm:w-auto"
        >
          Add another step
        </Button>
      </div>
    </div>
  );
};

// ─── One action card ───────────────────────────────
const ActionCard = ({
  index, action, isFirst, isLast, canRemove, error,
  onPatch, onRemove, onDuplicate, onMoveUp, onMoveDown,
}) => {
  const Icon = ICON_FOR_TYPE[action.type] || MessageCircle;

  return (
    <li
      className={cn(
        'rounded-2xl border border-ink-100 bg-white p-4 shadow-soft dark:border-ink-800 dark:bg-ink-900',
        error && 'border-red-300 dark:border-red-500/40'
      )}
    >
      {/* Header: step number + actions */}
      <div className="mb-3 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-sm font-medium text-ink-700 dark:text-ink-200">
          <span className="grid h-6 w-6 place-items-center rounded-md bg-ink-100 text-xs dark:bg-ink-800">
            <Icon size={14} />
          </span>
          <span>Step {index + 1}</span>
        </div>
        <div className="flex items-center gap-1">
          <IconButton
            aria-label="Move up"
            title="Move up"
            onClick={onMoveUp}
            disabled={isFirst}
          >
            <ChevronUp size={16} />
          </IconButton>
          <IconButton
            aria-label="Move down"
            title="Move down"
            onClick={onMoveDown}
            disabled={isLast}
          >
            <ChevronDown size={16} />
          </IconButton>
          <IconButton
            aria-label="Duplicate this step"
            title="Duplicate"
            onClick={onDuplicate}
          >
            <Copy size={16} />
          </IconButton>
          <IconButton
            aria-label="Remove this step"
            title={canRemove ? 'Remove' : 'At least one step required'}
            onClick={onRemove}
            disabled={!canRemove}
          >
            <Trash2 size={16} />
          </IconButton>
        </div>
      </div>

      {/* Type picker — compact horizontal row */}
      <div className="mb-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
        {PICKER_OPTIONS.map(({ value, label, icon: ItemIcon }) => {
          const selected = action.type === value;
          return (
            <button
              key={value}
              type="button"
              onClick={() => onPatch({ type: value })}
              className={cn(
                'flex items-center gap-2 rounded-xl border px-3 py-2 text-sm font-medium transition-colors',
                selected
                  ? 'border-brand-300 bg-brand-50 text-brand-700 dark:border-brand-500/40 dark:bg-brand-500/10 dark:text-brand-300'
                  : 'border-ink-200 bg-white text-ink-700 hover:bg-ink-50 dark:border-ink-800 dark:bg-ink-900 dark:text-ink-200 dark:hover:bg-ink-800/40'
              )}
            >
              <ItemIcon size={16} />
              <span className="truncate">{label}</span>
            </button>
          );
        })}
      </div>

      {/* Type-specific fields */}
      {(action.type === ACTION_TYPE.SEND_MESSAGE
        || action.type === ACTION_TYPE.SEND_DM) && (
        <MessageField action={action} onPatch={onPatch} />
      )}

      {action.type === ACTION_TYPE.SEND_LINK && (
        <>
          <MessageField action={action} onPatch={onPatch} />
          <Field label="Link URL" required></Field>
            <input
              type="url"
              className="input"
              placeholder="https://your-link.com"
              value={action.link || ''}
              onChange={(e) => onPatch({ link: e.target.value })}
            />
          </FormField>
        </>
      )}

      {action.type === ACTION_TYPE.DELAY && (
        <DelayField action={action} onPatch={onPatch} />
      )}

      {action.type === ACTION_TYPE.SAVE_CONTACT && (
        <p className="rounded-xl bg-ink-50 px-3 py-2.5 text-sm text-ink-600 dark:bg-ink-800/40 dark:text-ink-300">
          The sender will be added (or refreshed) in your Contacts list.
          No message will be sent.
        </p>
      )}

      {error && (
        <p className="mt-3 text-sm text-red-600 dark:text-red-400">{error}</p>
      )}
    </li>
  );
};

// ─── Message + Link reusable bits ──────────────────
const MessageField = ({ action, onPatch }) => {
  // Each MessageField owns its own modal-open state. Multi-action
  // chains can have several SEND_* cards on screen at once; per-card
  // state keeps them independent without lifting anything into the
  // store (the modal is purely transient UI, not draft state).
  const [aiOpen, setAiOpen] = useState(false);

  return (
    <>
      <Field label="Message" required></Field>
        <TextArea
          value={action.message || ''}
          onChange={(e) => onPatch({ message: e.target.value })}
          placeholder="Hey {{username}} 👋"
          rows={3}
        />
        <div className="mt-1.5 flex items-start justify-between gap-3">
          <p className="text-xs text-ink-500 dark:text-ink-400">
            Use{' '}
            <code className="rounded bg-ink-100 px-1.5 py-0.5 font-mono text-[11px] dark:bg-ink-800">{`{{username}}`}</code>
            {' '}to personalise with the recipient handle.
          </p>
          <button
            type="button"
            onClick={() => setAiOpen(true)}
            className={cn(
              'inline-flex shrink-0 items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-medium',
              'text-brand-700 hover:bg-brand-50 dark:text-brand-300 dark:hover:bg-brand-500/10',
              'transition-colors'
            )}
            title="Generate suggestions with AI"
          >
            <Sparkles size={14} />
            Generate with AI
          </button>
        </div>
      </FormField>

      <AiAssistantModal
        open={aiOpen}
        onClose={() => setAiOpen(false)}
        onInsert={(text) => onPatch({ message: text })}
      />
    </>
  );
};

// ─── Delay: amount + unit ──────────────────────────
const DelayField = ({ action, onPatch }) => {
  // Display in minutes when the saved value is a clean multiple ≥ 60s;
  // otherwise show seconds. We always store seconds on the wire.
  const seconds = Number(action.delaySeconds) || 0;
  const showMinutes = seconds >= 60 && seconds % 60 === 0;
  const displayValue = showMinutes ? seconds / 60 : seconds;
  const unit = showMinutes ? 'minutes' : 'seconds';

  const handleAmountChange = (e) => {
    const n = Math.max(0, Number(e.target.value) || 0);
    const next = unit === 'minutes' ? n * 60 : n;
    onPatch({ delaySeconds: clamp(next) });
  };

  const handleUnitChange = (e) => {
    const nextUnit = e.target.value;
    // Convert to keep the human-displayed number stable across the toggle.
    if (nextUnit === unit) return;
    const inSeconds = nextUnit === 'minutes' ? displayValue * 60 : displayValue;
    onPatch({ delaySeconds: clamp(inSeconds) });
  };

  return (
    <Field label="Wait" required>
      <div className="flex items-center gap-2">
        <input
          type="number"
          min={unit === 'minutes' ? 1 : DELAY_MIN_SECONDS}
          max={unit === 'minutes' ? DELAY_MAX_SECONDS / 60 : DELAY_MAX_SECONDS}
          step={1}
          className="input w-32"
          value={displayValue || ''}
          onChange={handleAmountChange}
        />
        <select
          className="input w-32"
          value={unit}
          onChange={handleUnitChange}
        >
          <option value="seconds">seconds</option>
          <option value="minutes">minutes</option>
        </select>
        <span className="text-sm text-ink-500 dark:text-ink-400">
          before the next step
        </span>
      </div>
      <p className="mt-1 text-xs text-ink-500 dark:text-ink-400">
        Maximum 24 hours. The chain pauses without holding a worker thread.
      </p>
    </Field>
  );
};

const clamp = (n) => Math.max(DELAY_MIN_SECONDS, Math.min(DELAY_MAX_SECONDS, Math.round(n)));

export default ActionStep;
