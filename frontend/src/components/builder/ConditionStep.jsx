import { Layers, KeyRound, Zap } from 'lucide-react';
import RadioCardGroup from '../ui/RadioCardGroup.jsx';
import FormField from '../form/FormField.jsx';
import { useBuilderStore } from '../../store/builderStore.js';
import { CONDITION_TYPE, MATCH_TYPE, TRIGGER_TYPE } from '../../utils/constants.js';
import { cn } from '../../utils/helpers.js';

// Triggers that carry no text — keyword matching is not applicable.
const NO_KEYWORD_TRIGGERS = new Set([
  TRIGGER_TYPE.STORY_MENTION,
  TRIGGER_TYPE.CONTENT_SHARED,
]);

const CONDITION_OPTIONS = [
  {
    value: CONDITION_TYPE.ANY,
    label: 'Any comment/message',
    description: 'Fire for every matching event without further filtering.',
    icon: Layers,
    tone: 'neutral',
  },
  {
    value: CONDITION_TYPE.KEYWORD,
    label: 'Keyword match',
    description: 'Only fire when the content matches a specific keyword.',
    icon: KeyRound,
    tone: 'brand',
  },
];

const MATCH_TYPES = [
  { value: MATCH_TYPE.CONTAINS, label: 'Contains',
    hint: 'Fires if the keyword appears anywhere in the message.' },
  { value: MATCH_TYPE.EXACT,    label: 'Exact match',
    hint: 'Fires only when the entire message equals the keyword.' },
];

const ConditionStep = ({ errors = {}, trigger }) => {
  const cond = useBuilderStore((s) => s.draft.condition);
  const setConditionType = useBuilderStore((s) => s.setConditionType);
  const setKeyword       = useBuilderStore((s) => s.setKeyword);
  const setMatchType     = useBuilderStore((s) => s.setMatchType);

  const isKeyword = cond.type === CONDITION_TYPE.KEYWORD;
  const noKeyword = NO_KEYWORD_TRIGGERS.has(trigger);

  // Auto-lock to ANY for triggers that carry no text content
  if (noKeyword && cond.type !== CONDITION_TYPE.ANY) {
    setConditionType(CONDITION_TYPE.ANY);
  }

  if (noKeyword) {
    const label = trigger === TRIGGER_TYPE.STORY_MENTION
      ? 'Every story mention'
      : 'Every shared post or reel';
    const desc = trigger === TRIGGER_TYPE.STORY_MENTION
      ? 'Fires whenever someone tags @you in their story. No text to filter — it always fires.'
      : 'Fires whenever someone shares a post or reel to your DMs. No text to filter — it always fires.';

    return (
      <div>
        <header className="mb-6">
          <h2 className="text-xl font-semibold text-ink-900 dark:text-ink-100">
            When should it fire?
          </h2>
          <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
            This trigger has no text content, so keyword filtering is not available.
          </p>
        </header>
        <div className="flex items-start gap-3 rounded-2xl border border-brand-200 bg-brand-50/60 p-4 dark:border-brand-800 dark:bg-brand-500/10">
          <Zap size={18} className="mt-0.5 shrink-0 text-brand-600 dark:text-brand-400" />
          <div>
            <p className="text-sm font-semibold text-ink-900 dark:text-ink-100">{label}</p>
            <p className="mt-0.5 text-sm text-ink-500 dark:text-ink-400">{desc}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <header className="mb-6">
        <h2 className="text-xl font-semibold text-ink-900 dark:text-ink-100">
          When should it fire?
        </h2>
        <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
          Choose between firing on every event or only on keyword matches.
        </p>
      </header>

      <RadioCardGroup
        name="condition"
        options={CONDITION_OPTIONS}
        value={cond.type}
        onChange={setConditionType}
        columns={2}
      />

      {isKeyword && (
        <div className="mt-6 space-y-4 rounded-2xl border border-ink-100 bg-ink-50/50 p-4 dark:border-ink-800 dark:bg-ink-800/30">
          <FormField
            label="Keyword"
            placeholder='e.g. "link"'
            value={cond.keyword}
            onChange={(e) => setKeyword(e.target.value)}
            hint="The text we'll look for in the incoming message."
            error={errors.keyword}
          />

          <div>
            <p className="label">Match type</p>
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              {MATCH_TYPES.map((m) => {
                const selected = cond.matchType === m.value;
                return (
                  <button
                    key={m.value}
                    type="button"
                    onClick={() => setMatchType(m.value)}
                    className={cn(
                      'flex flex-col items-start rounded-xl border px-4 py-3 text-left transition-colors',
                      selected
                        ? 'border-brand-500 bg-white ring-2 ring-brand-500/30 dark:border-brand-400 dark:bg-ink-900 dark:ring-brand-400/30'
                        : 'border-ink-200 bg-white hover:border-ink-300 dark:border-ink-800 dark:bg-ink-900 dark:hover:border-ink-700'
                    )}
                  >
                    <span className="text-sm font-semibold text-ink-900 dark:text-ink-100">
                      {m.label}
                    </span>
                    <span className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">
                      {m.hint}
                    </span>
                  </button>
                );
              })}
            </div>
            {errors.matchType && (
              <p className="error-text">{errors.matchType}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ConditionStep;
