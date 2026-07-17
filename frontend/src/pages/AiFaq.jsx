import { useEffect, useState, useCallback } from 'react';
import { Plus, Trash2, Sparkles, Save, Lock } from 'lucide-react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';

import PageHeader from '../components/ui/PageHeader.jsx';
import Button from '../components/form/Button.jsx';
import FormField from '../components/form/FormField.jsx';
import Switch from '../components/ui/Switch.jsx';
import aiFaqService from '../services/aiFaqService.js';
import { ROUTES } from '../utils/constants.js';
import { useAccountStore } from '../store/accountStore.js';

const MAX_QA_PAIRS = 50;
const MAX_KB_CHARS = 8000;

const emptyPair = () => ({ question: '', answer: '' });

const AiFaq = () => {
  const activeAccount = useAccountStore((s) => s.activeAccount);

  const [enabled, setEnabled] = useState(false);
  const [knowledgeBase, setKnowledgeBase] = useState('');
  const [qaPairs, setQaPairs] = useState([emptyPair()]);
  const [planEligible, setPlanEligible] = useState(true);
  const [plan, setPlan] = useState('FREE');

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await aiFaqService.get();
      setEnabled(!!data.enabled);
      setKnowledgeBase(data.knowledgeBase || '');
      setQaPairs(data.qaPairs && data.qaPairs.length > 0 ? data.qaPairs : [emptyPair()]);
      setPlanEligible(!!data.planEligible);
      setPlan(data.plan || 'FREE');
    } catch (err) {
      setError('Failed to load AI FAQ settings.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load, activeAccount?.instagramUserId]);

  const handlePairChange = (index, field, value) => {
    setQaPairs((prev) => prev.map((p, i) => (i === index ? { ...p, [field]: value } : p)));
  };

  const handleAddPair = () => {
    if (qaPairs.length >= MAX_QA_PAIRS) return;
    setQaPairs((prev) => [...prev, emptyPair()]);
  };

  const handleRemovePair = (index) => {
    setQaPairs((prev) => {
      const next = prev.filter((_, i) => i !== index);
      return next.length === 0 ? [emptyPair()] : next;
    });
  };

  const handleSave = async () => {
    if (!planEligible) return;

    setIsSaving(true);
    try {
      const payload = {
        enabled,
        knowledgeBase: knowledgeBase.slice(0, MAX_KB_CHARS),
        qaPairs: qaPairs.filter((p) => p.question.trim().length > 0)
          .map((p) => ({ question: p.question.trim(), answer: p.answer?.trim() || '' })),
      };
      await aiFaqService.save(payload);
      toast.success('AI FAQ settings saved.');
      await load();
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to save AI FAQ settings.');
    } finally {
      setIsSaving(false);
    }
  };

  if (!activeAccount) {
    return (
      <div className="mx-auto max-w-2xl">
        <PageHeader
          title="AI FAQ"
          description="Let AI answer DMs that don't match a keyword automation."
        />
        <div className="card p-8 text-center">
          <Sparkles size={36} className="mx-auto mb-3 text-ink-300 dark:text-ink-600" />
          <p className="text-sm text-ink-500 dark:text-ink-400">
            Connect an Instagram account first to set up AI FAQ.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="AI FAQ"
        description="When a DM doesn't match any keyword automation, Gemini answers using the Q&A and business info below."
        actions={
          <Button
            leftIcon={Save}
            onClick={handleSave}
            isLoading={isSaving}
            disabled={isLoading || !planEligible}
          >
            Save
          </Button>
        }
      />

      {!isLoading && !planEligible && (
        <div className="mb-6 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/60 p-4 dark:border-amber-800 dark:bg-amber-500/10">
          <Lock size={18} className="mt-0.5 shrink-0 text-amber-600 dark:text-amber-400" />
          <div className="flex-1">
            <p className="text-sm font-medium text-amber-800 dark:text-amber-300">
              AI FAQ is a Pro feature.
            </p>
            <p className="mt-0.5 text-sm text-amber-700 dark:text-amber-400">
              You're on the {plan === 'FREE' ? 'Free' : plan} plan. Upgrade to Pro to enable AI-powered
              DM answers.
            </p>
            <Link
              to={ROUTES.SUBSCRIPTION}
              className="mt-2 inline-block text-sm font-semibold text-amber-800 underline dark:text-amber-300"
            >
              View plans →
            </Link>
          </div>
        </div>
      )}

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
        <fieldset disabled={!planEligible} className={!planEligible ? 'opacity-60' : ''}>
          <div className="card mb-4 flex items-center justify-between p-4">
            <div>
              <p className="text-sm font-semibold text-ink-900 dark:text-ink-100">Enable AI FAQ</p>
              <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">
                Runs only when no keyword automation matches the DM.
              </p>
            </div>
            <Switch checked={enabled} onChange={setEnabled} srLabel="Enable AI FAQ" />
          </div>

          <div className="card mb-4 p-4">
            <label className="label">Business info (knowledge base)</label>
            <textarea
              className="input min-h-[140px] resize-y"
              placeholder="e.g. We sell handmade candles, ship worldwide in 3-5 days, prices start at $18. Returns accepted within 14 days..."
              value={knowledgeBase}
              maxLength={MAX_KB_CHARS}
              onChange={(e) => setKnowledgeBase(e.target.value)}
            />
            <p className="mt-1 text-right text-xs text-ink-400 dark:text-ink-500">
              {MAX_KB_CHARS - knowledgeBase.length} chars left
            </p>
          </div>

          <p className="mb-2 px-1 text-sm font-semibold text-ink-800 dark:text-ink-200">
            Curated Q&amp;A (checked first)
          </p>
          <div className="space-y-3">
            {qaPairs.map((p, index) => (
              <div key={index} className="card flex gap-3 p-4">
                <div className="min-w-0 flex-1 space-y-3">
                  <FormField
                    label="Question"
                    placeholder='e.g. "Do you ship internationally?"'
                    value={p.question}
                    onChange={(e) => handlePairChange(index, 'question', e.target.value)}
                  />
                  <FormField
                    label="Answer"
                    placeholder="e.g. Yes! We ship worldwide, 3-5 business days."
                    value={p.answer}
                    onChange={(e) => handlePairChange(index, 'answer', e.target.value)}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => handleRemovePair(index)}
                  className="shrink-0 self-start rounded-lg p-1.5 text-ink-400 transition-colors hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-900/20"
                  title="Remove"
                >
                  <Trash2 size={15} />
                </button>
              </div>
            ))}

            {qaPairs.length < MAX_QA_PAIRS && (
              <button
                type="button"
                onClick={handleAddPair}
                className="flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-ink-200 py-4 text-sm font-medium text-ink-400 transition-colors hover:border-brand-400 hover:text-brand-600 dark:border-ink-700 dark:text-ink-500 dark:hover:border-brand-600 dark:hover:text-brand-400"
              >
                <Plus size={16} />
                Add Q&amp;A ({qaPairs.length}/{MAX_QA_PAIRS})
              </button>
            )}
          </div>
        </fieldset>
      )}
    </div>
  );
};

export default AiFaq;
