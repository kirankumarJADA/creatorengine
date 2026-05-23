import { useState } from 'react';
import { Sparkles, RefreshCw, Check, AlertTriangle, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

import Modal from '../ui/Modal.jsx';
import Button from '../form/Button.jsx';
import FormField from '../form/FormField.jsx';
import Badge from '../ui/Badge.jsx';
import {
  MESSAGE_TONE,
  MESSAGE_TONE_LABEL,
} from '../../utils/constants.js';
import aiMessageService from '../../services/aiMessageService.js';
import { cn } from '../../utils/helpers.js';

/**
 * "Generate with AI" modal — wired up next to each MessageField in
 * ActionStep. Collects the user's brief, calls the backend, shows
 * 3 suggestions; the user picks one which fills the parent field
 * (via the {@code onInsert} callback) and closes the modal.
 *
 * No retry-on-error UX needed — the backend's provider chain
 * guarantees a non-error response (either LLM-quality or
 * template-fallback). The fallback case is signalled via
 * {@code result.provider} so we can show a subtle badge.
 */
const TONE_OPTIONS = [
  MESSAGE_TONE.FRIENDLY,
  MESSAGE_TONE.PROFESSIONAL,
  MESSAGE_TONE.SALES,
  MESSAGE_TONE.CASUAL,
];

const AiAssistantModal = ({ open, onClose, onInsert }) => {
  // ─── Form state ──────────────────────────────────
  const [goal,     setGoal]     = useState('');
  const [tone,     setTone]     = useState(MESSAGE_TONE.FRIENDLY);
  const [audience, setAudience] = useState('');
  const [cta,      setCta]      = useState('');

  // ─── Result state ────────────────────────────────
  const [result,   setResult]   = useState(null);   // { suggestions, provider }
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState(null);

  const canSubmit = goal.trim().length > 0
                 && audience.trim().length > 0
                 && tone
                 && !loading;

  const handleGenerate = async () => {
    if (!canSubmit) return;
    setLoading(true);
    setError(null);
    try {
      const data = await aiMessageService.generate({
        goal:     goal.trim(),
        tone,
        audience: audience.trim(),
        cta:      cta.trim() || null,
      });
      setResult(data);
    } catch (err) {
      const msg = err?.response?.data?.message
        || err?.message
        || 'Could not generate suggestions. Please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleInsert = (suggestion) => {
    onInsert?.(suggestion);
    toast.success('Inserted into message field.');
    handleClose();
  };

  const handleClose = () => {
    // Reset everything so the next open is clean.
    setGoal('');
    setTone(MESSAGE_TONE.FRIENDLY);
    setAudience('');
    setCta('');
    setResult(null);
    setError(null);
    setLoading(false);
    onClose?.();
  };

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="Generate with AI"
      description="Describe your goal and we'll write 3 DM templates you can refine."
      size="lg"
      footer={
        <>
          <Button variant="secondary" onClick={handleClose}>Close</Button>
          {result ? (
            <Button
              leftIcon={loading ? Loader2 : RefreshCw}
              onClick={handleGenerate}
              disabled={!canSubmit}
              isLoading={loading}
            >
              Regenerate
            </Button>
          ) : (
            <Button
              leftIcon={loading ? Loader2 : Sparkles}
              onClick={handleGenerate}
              disabled={!canSubmit}
              isLoading={loading}
            >
              Generate
            </Button>
          )}
        </>
      }
    >
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-[1fr_1.2fr]">
        {/* ─── Brief inputs ─────────────────────────── */}
        <div className="space-y-4">
          <FormField label="Goal" required hint="What is the DM trying to do?">
            <input
              type="text"
              className="input"
              placeholder="Send free guide on Instagram growth"
              value={goal}
              onChange={(e) => setGoal(e.target.value)}
              maxLength={500}
            />
          </FormField>

          <FormField label="Tone" required>
            <select
              className="input"
              value={tone}
              onChange={(e) => setTone(e.target.value)}
            >
              {TONE_OPTIONS.map((t) => (
                <option key={t} value={t}>{MESSAGE_TONE_LABEL[t]}</option>
              ))}
            </select>
          </FormField>

          <FormField label="Audience" required hint="Who's commenting on your posts?">
            <input
              type="text"
              className="input"
              placeholder="Aspiring creators, students, customers…"
              value={audience}
              onChange={(e) => setAudience(e.target.value)}
              maxLength={200}
            />
          </FormField>

          <FormField label="Call to action" hint="Optional — leave blank if there isn't one.">
            <input
              type="text"
              className="input"
              placeholder="Tap the link to join the free workshop"
              value={cta}
              onChange={(e) => setCta(e.target.value)}
              maxLength={200}
            />
          </FormField>
        </div>

        {/* ─── Suggestions pane ──────────────────────── */}
        <div className="min-h-[300px] rounded-2xl border border-ink-100 bg-ink-50/40 p-4 dark:border-ink-800 dark:bg-ink-900/40">
          {!result && !loading && !error && (
            <EmptyHint />
          )}
          {loading && (
            <LoadingState />
          )}
          {error && !loading && (
            <ErrorState message={error} />
          )}
          {result && !loading && (
            <SuggestionList
              result={result}
              onInsert={handleInsert}
            />
          )}
        </div>
      </div>
    </Modal>
  );
};

// ─── Subcomponents ─────────────────────────────────
const EmptyHint = () => (
  <div className="flex h-full flex-col items-center justify-center gap-2 text-center text-sm text-ink-500 dark:text-ink-400">
    <span className="grid h-10 w-10 place-items-center rounded-full bg-brand-50 text-brand-600 dark:bg-brand-500/10 dark:text-brand-300">
      <Sparkles size={18} />
    </span>
    <p className="font-medium text-ink-700 dark:text-ink-200">
      Fill in the brief and click Generate.
    </p>
    <p className="max-w-xs">
      We'll write 3 short DM templates targeting the tone and audience you picked.
    </p>
  </div>
);

const LoadingState = () => (
  <div className="flex h-full flex-col items-center justify-center gap-3 text-sm text-ink-500 dark:text-ink-400">
    <Loader2 size={20} className="animate-spin text-brand-600 dark:text-brand-400" />
    <p>Writing suggestions…</p>
  </div>
);

const ErrorState = ({ message }) => (
  <div className="flex h-full flex-col items-center justify-center gap-3 text-center text-sm">
    <AlertTriangle size={20} className="text-red-500" />
    <p className="font-medium text-red-700 dark:text-red-300">
      Couldn't generate suggestions
    </p>
    <p className="max-w-xs text-ink-500 dark:text-ink-400">{message}</p>
  </div>
);

const SuggestionList = ({ result, onInsert }) => {
  const { suggestions = [], provider } = result;
  const isFallback = provider === 'fallback';

  if (!suggestions.length) {
    return <ErrorState message="No suggestions were returned." />;
  }
  return (
    <div className="flex h-full flex-col gap-3">
      {isFallback && (
        <div className="flex items-start gap-2 rounded-xl bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:bg-amber-500/10 dark:text-amber-200">
          <AlertTriangle size={14} className="mt-0.5 shrink-0" />
          <p>
            Showing template-based suggestions (AI provider unavailable). Edit before saving.
          </p>
        </div>
      )}
      {suggestions.map((text, i) => (
        <SuggestionCard
          key={i}
          index={i}
          text={text}
          onInsert={() => onInsert(text)}
        />
      ))}
    </div>
  );
};

const TONE_BADGE_LABELS = ['Friendly', 'Professional', 'Sales-focused'];

const SuggestionCard = ({ index, text, onInsert }) => (
  <div className={cn(
    'rounded-xl border border-ink-100 bg-white p-3 shadow-soft transition-colors',
    'hover:border-brand-200 dark:border-ink-800 dark:bg-ink-900 dark:hover:border-brand-500/40'
  )}>
    <div className="mb-2 flex items-center justify-between gap-2">
      <Badge tone="neutral">
        {TONE_BADGE_LABELS[index] || `Suggestion ${index + 1}`}
      </Badge>
      <Button
        size="sm"
        variant="secondary"
        leftIcon={Check}
        onClick={onInsert}
      >
        Use this
      </Button>
    </div>
    <p className="whitespace-pre-wrap text-sm text-ink-800 dark:text-ink-200">
      {text}
    </p>
  </div>
);

export default AiAssistantModal;
