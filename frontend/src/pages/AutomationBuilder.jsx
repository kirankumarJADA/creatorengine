import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowLeft, ArrowRight, Save, X } from 'lucide-react';
import toast from 'react-hot-toast';

import StepIndicator from '../components/builder/StepIndicator.jsx';
import TriggerStep   from '../components/builder/TriggerStep.jsx';
import PostPicker    from '../components/builder/PostPicker.jsx';
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
  TRIGGER_TYPE,
  TRIGGER_LABEL,
  ACTION_TYPE,
  ACTION_LABEL,
  POST_TARGET_MODE,
} from '../utils/constants.js';

// Public reply only makes sense for comment-based triggers.
const PUBLIC_REPLY_TRIGGERS = new Set([TRIGGER_TYPE.COMMENT, TRIGGER_TYPE.NEXT_POST]);

// Follow gate works for any DM-capable trigger (not live comments).
const FOLLOW_GATE_TRIGGERS = new Set([
  TRIGGER_TYPE.COMMENT,
  TRIGGER_TYPE.NEXT_POST,
  TRIGGER_TYPE.DM,
  TRIGGER_TYPE.STORY_REPLY,
  TRIGGER_TYPE.CONTENT_SHARED,
  TRIGGER_TYPE.STORY_MENTION,
]);

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

  const getAutomationById   = useAutomationStore((s) => s.getById);
  const fetchAutomationById = useAutomationStore((s) => s.fetchById);
  const createAutomation    = useAutomationStore((s) => s.createAutomation);
  const updateAutomation    = useAutomationStore((s) => s.updateAutomation);

  const [errors, setErrors] = useState({});
  const [isSaving, setIsSaving] = useState(false);
  const [isHydrating, setIsHydrating] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const hydrate = async () => {
      if (isEditing) {
        let existing = getAutomationById(id);
        if (!existing) {
          setIsHydrating(true);
          existing = await fetchAutomationById(id);
          if (cancelled) return;
          setIsHydrating(false);
        }
        if (!existing) {
          toast.error('Automation not found.');
          navigate(ROUTES.AUTOMATIONS, { replace: true });
          return;
        }
        startEdit(existing);
      } else {
        startCreate();
      }
    };

    hydrate();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => () => reset(), [reset]);

  const stepErrors = useMemo(() => {
    const { errors: full } = validateAutomationDraft(draft);
    switch (builderStep) {
      case 1: return { trigger:   full.trigger };
      case 2: return { condition: full.condition, keyword: full.keyword, matchType: full.matchType };
      case 3: return { actionsChain: full.actionsChain, actions: full.actions };
      case 4: return {};
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

  const handleSave = async () => {
    const { isValid, errors: allErrors } = validateAutomationDraft(draft);
    if (!isValid) {
      setErrors(allErrors);
      toast.error('Some fields still need attention.');
      const firstStep = stepOf(allErrors);
      if (firstStep) goToStep(firstStep);
      return;
    }

    const publicReplyEnabled = PUBLIC_REPLY_TRIGGERS.has(draft.trigger) && draft.publicReplyEnabled === true;
    const publicReplies = (draft.publicReplies || [])
      .filter((r) => r.text && r.text.trim())
      .map((r) => ({ text: r.text.trim(), enabled: r.enabled !== false }));

    if (publicReplyEnabled && !publicReplies.some((r) => r.enabled)) {
      toast.error('Add at least one active public reply, or turn off public replies.');
      goToStep(4);
      return;
    }

    const followGateEnabled = FOLLOW_GATE_TRIGGERS.has(draft.trigger) && draft.followGateEnabled === true;
    const followGateMessage = (draft.followGateMessage || '').trim();
    const followGateButtonLabel = (draft.followGateButtonLabel || '').trim();

    if (followGateEnabled && !followGateMessage) {
      toast.error('Add a follow message, or turn off "Ask to follow first".');
      goToStep(4);
      return;
    }

    // Follow-up message validation (single no-reply follow-up)
    if (draft.followUpEnabled) {
      if (!draft.followUpMessage || !draft.followUpMessage.trim()) {
        toast.error('Add a follow-up message, or turn off the follow-up.');
        goToStep(4);
        return;
      }
      if (!draft.followUpDelayAmount || draft.followUpDelayAmount <= 0) {
        toast.error('Follow-up delay must be greater than 0.');
        goToStep(4);
        return;
      }
    }

    // Strip empty/no-op actions: SEND_DM/SEND_MESSAGE with no message body
    // AND no image is a "no-op" the user left behind — drop it so the
    // backend isn't asked to send blank DMs. An image-only action (message
    // blank, imageUrl set) is valid and must survive this filter.
    // SEND_LINK still needs a link to count as real.
    const cleanedActions = (draft.actions || []).filter((a) => {
      if (!a || !a.type) return false;
      const hasImage = a.imageUrl && a.imageUrl.trim().length > 0;
      switch (a.type) {
        case ACTION_TYPE.SEND_DM:
        case ACTION_TYPE.SEND_MESSAGE:
          return (a.message && a.message.trim().length > 0) || hasImage;
        case ACTION_TYPE.SEND_LINK:
          return a.link && a.link.trim().length > 0;
        default:
          return true;
      }
    });

    // The automation must DO something: send a DM, a public reply, or
    // a follow gate. Otherwise saving an empty automation is pointless.
    if (cleanedActions.length === 0 && !publicReplyEnabled && !followGateEnabled) {
      toast.error('Add a DM, a public reply, or a follow gate — the automation has to do something.');
      goToStep(3);
      return;
    }

    // Resolve target-post mode + id.
    let targetPostMode;
    let targetPostId;
    if (draft.trigger === TRIGGER_TYPE.NEXT_POST) {
      targetPostMode = POST_TARGET_MODE.NEXT_POST;
      targetPostId = null;
    } else if (
      draft.trigger === TRIGGER_TYPE.COMMENT ||
      draft.trigger === TRIGGER_TYPE.CONTENT_SHARED
    ) {
      targetPostMode = draft.targetPostMode || POST_TARGET_MODE.ALL;
      targetPostId = targetPostMode === POST_TARGET_MODE.SPECIFIC
        ? (draft.targetPostId ?? null)
        : null;
    } else {
      targetPostMode = POST_TARGET_MODE.ALL;
      targetPostId = null;
    }

    setIsSaving(true);
    const payload = {
      name: draft.name?.trim() || autoName(draft),
      trigger: draft.trigger,
      targetPostMode,
      targetPostId,
      condition: draft.condition,
      actions: cleanedActions,
      publicReplyEnabled,
      publicReplies,
      followGateEnabled,
      followGateMessage,
      followGateButtonLabel,
      botProtectionEnabled: draft.botProtectionEnabled === true,
      botProtectionMinDelaySeconds: draft.botProtectionMinDelaySeconds ?? 2,
      botProtectionMaxDelaySeconds: draft.botProtectionMaxDelaySeconds ?? 8,
      followUpEnabled: draft.followUpEnabled === true,
      followUpDelayAmount: draft.followUpDelayAmount ?? 1,
      followUpDelayUnit: draft.followUpDelayUnit ?? 'HOURS',
      followUpMessage: draft.followUpEnabled ? (draft.followUpMessage || '').trim() : '',
      emailCollectEnabled: draft.emailCollectEnabled === true,
      emailCollectMessage: draft.emailCollectEnabled ? (draft.emailCollectMessage || '').trim() : '',
      enabled: draft.enabled,
    };

    try {
      if (isEditing) {
        await updateAutomation(id, payload);
        toast.success('Automation updated.');
      } else {
        await createAutomation(payload);
        const msg = draft.trigger === TRIGGER_TYPE.NEXT_POST
          ? 'Automation created. It will start working on your next post.'
          : 'Automation created.';
        toast.success(msg);
      }
      navigate(ROUTES.AUTOMATIONS);
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancel = () => navigate(ROUTES.AUTOMATIONS);

  if (isEditing && isHydrating) {
    return (
      <div className="mx-auto max-w-5xl">
        <div className="mb-6 h-8 w-64 animate-pulse rounded-lg bg-ink-100 dark:bg-ink-800" />
        <div className="card mb-5 p-5">
          <div className="h-6 w-full animate-pulse rounded-lg bg-ink-100 dark:bg-ink-800" />
        </div>
        <div className="card p-8">
          <div className="h-64 w-full animate-pulse rounded-lg bg-ink-100 dark:bg-ink-800" />
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl">
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

      <div className="card mb-5 p-4 sm:p-5">
        <StepIndicator current={builderStep} onStepClick={goToStep} />
      </div>

      <div className="card p-5 sm:p-8">
        <AnimatePresence mode="wait">
          <motion.div
            key={builderStep}
            initial={{ opacity: 0, x: 12 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -12 }}
            transition={{ duration: 0.2 }}
          >
            {builderStep === 1 && (
              <>
                <TriggerStep />
                <PostPicker />
              </>
            )}
            {builderStep === 2 && <ConditionStep errors={errors} />}
            {builderStep === 3 && <ActionStep errors={errors} />}
            {builderStep === 4 && <MessageStep errors={errors} />}
            {builderStep === 5 && <ReviewStep />}
          </motion.div>
        </AnimatePresence>

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

const stepOf = (errors) => {
  if (errors.trigger) return 1;
  if (errors.condition || errors.keyword || errors.matchType) return 2;
  if (errors.actionsChain || errors.actions || errors.action || errors.link) return 3;
  if (errors.message) return 4;
  return null;
};

const autoName = (draft) => {
  const trigger = TRIGGER_LABEL[draft.trigger] || 'Trigger';
  const firstActionType = Array.isArray(draft.actions) && draft.actions[0]?.type
    ? draft.actions[0].type
    : draft.action?.type;
  const action = ACTION_LABEL[firstActionType] || 'Action';
  return `${trigger} → ${action}`;
};

export default AutomationBuilder;