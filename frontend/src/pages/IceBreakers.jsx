import { useEffect, useState, useCallback } from 'react';
import { Plus, Trash2, MessageCircleQuestion, GripVertical, Save } from 'lucide-react';
import toast from 'react-hot-toast';

import PageHeader from '../components/ui/PageHeader.jsx';
import Button from '../components/form/Button.jsx';
import FormField from '../components/form/FormField.jsx';
import iceBreakerService from '../services/iceBreakerService.js';
import { useAccountStore } from '../store/accountStore.js';

const MAX_QUESTIONS = 4;
const MAX_TITLE_LENGTH = 80;

const emptyQuestion = () => ({ title: '', payload: '' });

const IceBreakers = () => {
  const activeAccount = useAccountStore((s) => s.activeAccount);

  const [questions, setQuestions] = useState([emptyQuestion()]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await iceBreakerService.getAll();
      if (data.length > 0) {
        setQuestions(data);
      } else {
        setQuestions([emptyQuestion()]);
      }
    } catch (err) {
      setError('Failed to load ice breakers.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load, activeAccount?.instagramUserId]);

  const handleChange = (index, field, value) => {
    setQuestions((prev) =>
      prev.map((q, i) => (i === index ? { ...q, [field]: value } : q))
    );
  };

  const handleAdd = () => {
    if (questions.length >= MAX_QUESTIONS) return;
    setQuestions((prev) => [...prev, emptyQuestion()]);
  };

  const handleRemove = (index) => {
    setQuestions((prev) => {
      const next = prev.filter((_, i) => i !== index);
      return next.length === 0 ? [emptyQuestion()] : next;
    });
  };

  const handleSave = async () => {
    const valid = questions.filter((q) => q.title.trim().length > 0);
    if (valid.length === 0) {
      // Clearing all — delete
      setIsSaving(true);
      try {
        await iceBreakerService.deleteAll();
        toast.success('Ice breakers cleared.');
        setQuestions([emptyQuestion()]);
      } catch {
        toast.error('Failed to clear ice breakers.');
      } finally {
        setIsSaving(false);
      }
      return;
    }

    setIsSaving(true);
    try {
      await iceBreakerService.save(valid.map((q) => ({
        title: q.title.trim(),
        payload: q.payload?.trim() || q.title.trim(),
      })));
      toast.success('Ice breakers saved! They may take a few minutes to appear in DMs.');
      await load();
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to save ice breakers.');
    } finally {
      setIsSaving(false);
    }
  };

  if (!activeAccount) {
    return (
      <div className="mx-auto max-w-2xl">
        <PageHeader
          title="Ice Breakers"
          description="Quick-reply buttons shown to users when they open your DMs for the first time."
        />
        <div className="card p-8 text-center">
          <MessageCircleQuestion size={36} className="mx-auto mb-3 text-ink-300 dark:text-ink-600" />
          <p className="text-sm text-ink-500 dark:text-ink-400">
            Connect an Instagram account first to manage ice breakers.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Ice Breakers"
        description="Quick-reply buttons shown to users when they open your DMs for the first time. When tapped, the button text is sent as a DM — triggering your DM automations."
        actions={
          <Button
            leftIcon={Save}
            onClick={handleSave}
            isLoading={isSaving}
            disabled={isLoading}
          >
            Save
          </Button>
        }
      />

      {/* Explainer */}
      <div className="mb-6 rounded-2xl border border-brand-200 bg-brand-50/60 p-4 dark:border-brand-800 dark:bg-brand-500/10">
        <p className="text-sm text-ink-600 dark:text-ink-300">
          You can add up to <strong>{MAX_QUESTIONS}</strong> questions. When a user taps a button
          in your DM inbox, Instagram sends that text as a DM — so pair each button with a{' '}
          <strong>DM keyword automation</strong> to respond automatically.
        </p>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <div
              key={i}
              className="h-20 animate-pulse rounded-2xl border border-ink-100 bg-ink-50 dark:border-ink-800 dark:bg-ink-800/30"
            />
          ))}
        </div>
      ) : error ? (
        <div className="card p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <Button variant="secondary" className="mt-4" onClick={load}>
            Retry
          </Button>
        </div>
      ) : (
        <div className="space-y-3">
          {questions.map((q, index) => (
            <QuestionCard
              key={index}
              index={index}
              question={q}
              total={questions.length}
              onChange={handleChange}
              onRemove={handleRemove}
            />
          ))}

          {questions.length < MAX_QUESTIONS && (
            <button
              type="button"
              onClick={handleAdd}
              className="flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-ink-200 py-4 text-sm font-medium text-ink-400 transition-colors hover:border-brand-400 hover:text-brand-600 dark:border-ink-700 dark:text-ink-500 dark:hover:border-brand-600 dark:hover:text-brand-400"
            >
              <Plus size={16} />
              Add question ({questions.length}/{MAX_QUESTIONS})
            </button>
          )}
        </div>
      )}
    </div>
  );
};

const QuestionCard = ({ index, question, total, onChange, onRemove }) => {
  const remaining = MAX_TITLE_LENGTH - (question.title?.length || 0);

  return (
    <div className="card flex gap-3 p-4">
      {/* Drag handle (visual only) */}
      <div className="flex shrink-0 flex-col items-center justify-center text-ink-300 dark:text-ink-600">
        <GripVertical size={16} />
        <span className="mt-1 text-xs font-medium text-ink-400 dark:text-ink-500">
          {index + 1}
        </span>
      </div>

      {/* Fields */}
      <div className="min-w-0 flex-1 space-y-3">
        <div>
          <FormField
            label="Button text"
            placeholder='e.g. "What do you sell?"'
            value={question.title}
            onChange={(e) => onChange(index, 'title', e.target.value)}
            maxLength={MAX_TITLE_LENGTH}
          />
          <p className="mt-1 text-right text-xs text-ink-400 dark:text-ink-500">
            {remaining} chars left
          </p>
        </div>

        <FormField
          label="Keyword to match (optional)"
          placeholder="Defaults to the button text if left blank"
          value={question.payload}
          onChange={(e) => onChange(index, 'payload', e.target.value)}
          hint="The exact text sent as a DM when the user taps this button. Must match a DM keyword automation."
        />
      </div>

      {/* Remove */}
      <button
        type="button"
        onClick={() => onRemove(index)}
        className="shrink-0 self-start rounded-lg p-1.5 text-ink-400 transition-colors hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-900/20"
        title="Remove"
      >
        <Trash2 size={15} />
      </button>
    </div>
  );
};

export default IceBreakers;
