import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowLeft, ArrowRight, Save, X } from 'lucide-react';
import toast from 'react-hot-toast';

import StepIndicator from '../components/builder/StepIndicator.jsx';
import TriggerStep   from '../components/builder/TriggerStep.jsx';
import ConditionStep from '../components/builder/ConditionStep.jsx';
import ActionStep    from '../components/builder/ActionStep.jsx';
import MessageStep   from '../components/builder/MessageStep.jsx';
import ReviewStep    from '../components/builder/ReviewStep.jsx';
import Button        from '../components/form/Button.jsx';

import { useBuilderStore, STEPS } from '../store/builderStore.js';
import { useAutomationStore } from '../store/automationStore.js';
import { validateAutomationDraft } from '../utils/automationEngine.js';
import {
  ROUTES,
  TRIGGER_LABEL,
  ACTION_LABEL,
} from '../utils/constants.js';

/**
 * The 5-step automation wizard.
 *
 * Mounted on two routes:
 *   /automations/new        → blank draft
 *   /automations/:id/edit   → seed draft from the saved automation
 *
 * Each step renders into the same shell so the nav, progress strip,
 * and footer are positioned identically across steps. Per-step
 * validation runs on "Continue"; the final "Save" runs the full
 * validator before hitting the store.
 */
const AutomationBuilder = () => {
  const navigate    = useNavigate();
  const { id }      = useParams();
  const isEditing   = Boolean(id);

  const builderStep = useBuilderStore((s) => s.step);
  const draft       = useBuilderStore((s) => s.draft);
  const startCreate = useBuilderStore((s) => s.startCreate);
  const startEdit   = useBuilderStore((s) => s.startEdit);
  const next        = useBuilderStore((s) => s.next);
  const prev        = useBuilderStore((s) => s.prev);
  const goToStep    = useBuilderStore((s) => s.goToStep);
  const reset       = useBuilderStore((s) => s.reset);

  const getAutomationById  = useAutomationStore((s) => s.getById);
  const createAutomation   = useAutomationStore((s) => s.createAutomation);
  const updateAutomation   = useAutomationStore((s) => s.updateAutomation);

  const [errors, setErrors] = useState({});
  const [isSaving, setIsSaving] = useState(false);

  // ─── Seed the draft on mount ─────────────────────
  useEffect(() => {
    if (isEditing) {
      const existing = getAutomationById(id);
      if (!existing) {
        // The store hasn't seen this id — bounce back to the list.
        toast.error('Automation not found.');
        navigate(ROUTES.AUTOMATIONS, { replace: true });
        return;
      }
      startEdit(existing);
    } else {
      startCreate();
    }
    // We intentionally only re-seed when the route changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  // Reset draft on unmount so a stale wizard doesn't leak into the
  // next "Create" click.
  useEffect(() => () => reset(), [reset]);

  // ─── Per-step gating ─────────────────────────────
  const stepErrors = useMemo(() => {
    const { errors: full } = validateAutomationDraft(draft);
    switch (builderStep) {
      case 1: return { trigger:   full.trigger };
      case 2: return { condition: full.condition, keyword: full.keyword, matchType: full.matchType };
      case 3: return { action:    full.action, link: full.link };
      case 4: return { message:   full.message };
      default: return {};
    }
  }, [builderStep, draft]);

  const canAdvance = useMemo(() => {
    const errsForStep = Object.values(stepErrors).filter(Boolean);
    return errsForStep.length === 0;
  }, [stepErrors]);

  const handleContinue = () => {
    if (!canAdvance) {
      setErrors(stepErrors);
      toast.error('Please complete this step before continuing.');
      return;
    }
    setErrors({});
    next();
  };

  const handleBack = () => {
    setErrors({});
    prev();
  };

  // ─── Save ───────────────────────────────────────
  const handleSave = async () => {
    const { isValid, errors: allErrors } = validateAutomationDraft(draft);
    if (!isValid) {
      setErrors(allErrors);
      toast.error('Some fields still need attention.');
      // Jump to the earliest step with errors
      const firstStep = stepOf(allErrors);
      if (firstStep) goToStep(firstStep);
      return;
    }

    setIsSaving(true);
    const payload = {
      name: draft.name?.trim() || autoName(draft),
      trigger: draft.trigger,
      condition: draft.condition,
      // Canonical chain — backend prefers this over legacy action+message.
      actions: draft.actions,
      enabled: draft.enabled,
    };

    try {
      if (isEditing) {
        await updateAutomation(id, payload);
        toast.success('Automation updated.');
      } else {
        await createAutomation(payload);
        toast.success('Automation created.');
      }
      navigate(ROUTES.AUTOMATIONS);
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancel = () => navigate(ROUTES.AUTOMATIONS);

  // ─── Render ──────────────────────────────────────
  return (
    <div className="mx-auto max-w-5xl">
      {/* Header */}
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-brand-700 dark:text-brand-400">
            Automation builder
          </p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight text-ink-900 dark:text-ink-100 sm:text-3xl">
            {isEditing ? 'Edit automation' : 'Create automation'}
          </h1>
        </div>
        <Button variant="ghost" leftIcon={X} onClick={handleCancel}>
          Cancel
        </Button>
      </div>

      {/* Progress */}
      <div className="card mb-5 p-4 sm:p-5">
        <StepIndicator current={builderStep} onStepClick={goToStep} />
      </div>

      {/* Step card */}
      <div className="card p-5 sm:p-8">
        <AnimatePresence mode="wait">
          <motion.div
            key={builderStep}
            initial={{ opacity: 0, x: 12 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -12 }}
            transition={{ duration: 0.2 }}
          >
            {builderStep === 1 && <TriggerStep />}
            {builderStep === 2 && <ConditionStep errors={errors} />}
            {builderStep === 3 && <ActionStep errors={errors} />}
            {builderStep === 4 && <MessageStep errors={errors} />}
            {builderStep === 5 && <ReviewStep />}
          </motion.div>
        </AnimatePresence>

        {/* Footer nav */}
        <div className="mt-8 flex items-center justify-between border-t border-ink-100 pt-5 dark:border-ink-800">
          <Button
            variant="secondary"
            leftIcon={ArrowLeft}
            onClick={handleBack}
            disabled={builderStep === 1}
          >
            Back
          </Button>

          <p className="hidden text-xs text-ink-500 dark:text-ink-400 sm:block">
            Step {builderStep} of {STEPS.length}
          </p>

          {builderStep < STEPS.length ? (
            <Button rightIcon={ArrowRight} onClick={handleContinue}>
              Continue
            </Button>
          ) : (
            <Button leftIcon={Save} onClick={handleSave} isLoading={isSaving}>
              {isEditing ? 'Save changes' : 'Save automation'}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

// Map an errors object to the earliest step that contains one of those keys.
const stepOf = (errors) => {
  if (errors.trigger) return 1;
  if (errors.condition || errors.keyword || errors.matchType) return 2;
  if (errors.actionsChain || errors.actions || errors.action || errors.link) return 3;
  if (errors.message) return 4;
  return null;
};

// Build a friendly default name when the user leaves the name blank.
const autoName = (draft) => {
  const trigger = TRIGGER_LABEL[draft.trigger] || 'Trigger';
  // First action of the chain — falls back to legacy single action if absent.
  const firstActionType = Array.isArray(draft.actions) && draft.actions[0]?.type
    ? draft.actions[0].type
    : draft.action?.type;
  const action = ACTION_LABEL[firstActionType] || 'Action';
  return `${trigger} → ${action}`;
};

export default AutomationBuilder;
