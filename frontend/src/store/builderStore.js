import { create } from 'zustand';
import {
  TRIGGER_TYPE,
  CONDITION_TYPE,
  MATCH_TYPE,
  ACTION_TYPE,
  POST_TARGET_MODE,
} from '../utils/constants.js';

export const STEPS = Object.freeze([
  { id: 1, key: 'trigger',   label: 'Trigger' },
  { id: 2, key: 'condition', label: 'Condition' },
  { id: 3, key: 'action',    label: 'Actions' },
  { id: 4, key: 'message',   label: 'Preview' },
  { id: 5, key: 'review',    label: 'Review' },
]);

export const blankAction = (type = ACTION_TYPE.SEND_MESSAGE) => ({
  type,
  message: '',
  link: '',
  variations: [],
  imageUrl: '',
  delaySeconds: type === ACTION_TYPE.DELAY ? 5 : null,
});

const emptyDraft = () => ({
  name: '',
  trigger: null,
  targetPostMode: POST_TARGET_MODE.ALL,
  targetPostId: null,
  condition: {
    type: CONDITION_TYPE.ANY,
    keyword: '',
    matchType: MATCH_TYPE.CONTAINS,
  },
  action: { type: ACTION_TYPE.SEND_DM, link: '' },
  message: '',
  actions: [blankAction(ACTION_TYPE.SEND_MESSAGE)],
  publicReplyEnabled: false,
  publicReplies: [],
  followGateEnabled: false,
  followGateMessage: '',
  followGateButtonLabel: '',
  botProtectionEnabled: false,
  botProtectionMinDelaySeconds: 2,
  botProtectionMaxDelaySeconds: 8,
  followUpEnabled: false,
  followUpDelayAmount: 1,
  followUpDelayUnit: 'HOURS',
  followUpMessage: '',
  enabled: true,
});

const normalizeActionsFromBackend = (automation) => {
  if (Array.isArray(automation.actions) && automation.actions.length > 0) {
    return automation.actions.map((a) => ({
      type:         a.type ?? ACTION_TYPE.SEND_MESSAGE,
      message:      a.message ?? '',
      link:         a.link ?? '',
      // FIX: variations was previously dropped here, so any variations
      // saved to the backend disappeared the moment you reopened the
      // automation to edit it. Now preserved as an array of strings.
      variations:   Array.isArray(a.variations) ? a.variations : [],
      imageUrl:     a.imageUrl ?? '',
      delaySeconds: a.delaySeconds ?? null,
    }));
  }
  if (automation.action) {
    return [{
      type:         automation.action.type ?? ACTION_TYPE.SEND_DM,
      message:      automation.message ?? '',
      link:         automation.action.link ?? '',
      variations:   Array.isArray(automation.action.variations) ? automation.action.variations : [],
      imageUrl:     automation.action.imageUrl ?? '',
      delaySeconds: null,
    }];
  }
  return [blankAction(ACTION_TYPE.SEND_MESSAGE)];
};

const normalizePublicReplies = (automation) => {
  if (!Array.isArray(automation.publicReplies)) return [];
  return automation.publicReplies.map((r) => ({
    text:    r.text ?? '',
    enabled: r.enabled !== false,
  }));
};

const normalizeTargetPostMode = (automation) => {
  if (automation.targetPostMode) return automation.targetPostMode;
  return automation.targetPostId ? POST_TARGET_MODE.SPECIFIC : POST_TARGET_MODE.ALL;
};

export const useBuilderStore = create((set, get) => ({
  step: 1,
  mode: 'create',
  editingId: null,
  draft: emptyDraft(),

  startCreate: () => set({
    step: 1, mode: 'create', editingId: null, draft: emptyDraft(),
  }),

  startEdit: (automation) => set({
    step: 1,
    mode: 'edit',
    editingId: automation.id,
    draft: {
      name:      automation.name || '',
      trigger:   automation.trigger ?? null,
      targetPostMode: normalizeTargetPostMode(automation),
      targetPostId: automation.targetPostId ?? null,
      condition: {
        type:      automation.condition?.type      ?? CONDITION_TYPE.ANY,
        keyword:   automation.condition?.keyword   ?? '',
        matchType: automation.condition?.matchType ?? MATCH_TYPE.CONTAINS,
      },
      action: {
        type: automation.action?.type ?? ACTION_TYPE.SEND_DM,
        link: automation.action?.link ?? '',
      },
      message: automation.message ?? '',
      actions: normalizeActionsFromBackend(automation),
      publicReplyEnabled: automation.publicReplyEnabled === true,
      publicReplies: normalizePublicReplies(automation),
      followGateEnabled: automation.followGateEnabled === true,
      followGateMessage: automation.followGateMessage ?? '',
      followGateButtonLabel: automation.followGateButtonLabel ?? '',
      botProtectionEnabled: automation.botProtectionEnabled === true,
      botProtectionMinDelaySeconds: automation.botProtectionMinDelaySeconds ?? 2,
      botProtectionMaxDelaySeconds: automation.botProtectionMaxDelaySeconds ?? 8,
      followUpEnabled: automation.followUpEnabled === true,
      followUpDelayAmount: automation.followUpDelayAmount ?? 1,
      followUpDelayUnit: automation.followUpDelayUnit ?? 'HOURS',
      followUpMessage: automation.followUpMessage ?? '',
      enabled: automation.enabled !== false,
    },
  }),

  reset: () => set({ step: 1, mode: 'create', editingId: null, draft: emptyDraft() }),

  goToStep: (step) => {
    const clamped = Math.max(1, Math.min(STEPS.length, step));
    set({ step: clamped });
  },
  next: () => set((s) => ({ step: Math.min(STEPS.length, s.step + 1) })),
  prev: () => set((s) => ({ step: Math.max(1, s.step - 1) })),

  setTrigger: (trigger) =>
    set((s) => ({ draft: { ...s.draft, trigger } })),

  /** Set the post-targeting mode and clear targetPostId when it no longer applies. */
  setTargetPostMode: (targetPostMode) =>
    set((s) => ({
      draft: {
        ...s.draft,
        targetPostMode,
        targetPostId: targetPostMode === POST_TARGET_MODE.SPECIFIC
          ? s.draft.targetPostId
          : null,
      },
    })),

  setTargetPostId: (targetPostId) =>
    set((s) => ({
      draft: {
        ...s.draft,
        targetPostId,
        targetPostMode: targetPostId
          ? POST_TARGET_MODE.SPECIFIC
          : (s.draft.targetPostMode === POST_TARGET_MODE.SPECIFIC
              ? POST_TARGET_MODE.ALL
              : s.draft.targetPostMode),
      },
    })),

  setConditionType: (type) =>
    set((s) => ({ draft: { ...s.draft, condition: { ...s.draft.condition, type } } })),

  setKeyword: (keyword) =>
    set((s) => ({ draft: { ...s.draft, condition: { ...s.draft.condition, keyword } } })),

  setMatchType: (matchType) =>
    set((s) => ({ draft: { ...s.draft, condition: { ...s.draft.condition, matchType } } })),

  setName: (name) =>
    set((s) => ({ draft: { ...s.draft, name } })),

  setEnabled: (enabled) =>
    set((s) => ({ draft: { ...s.draft, enabled } })),

  setPublicReplyEnabled: (publicReplyEnabled) =>
    set((s) => ({ draft: { ...s.draft, publicReplyEnabled } })),

  setPublicReplies: (publicReplies) =>
    set((s) => ({ draft: { ...s.draft, publicReplies } })),

  addPublicReply: (text = '') =>
    set((s) => {
      if (s.draft.publicReplies.length >= 10) return s;
      return {
        draft: {
          ...s.draft,
          publicReplies: [...s.draft.publicReplies, { text, enabled: true }],
        },
      };
    }),

  updatePublicReply: (index, patch) =>
    set((s) => {
      const next = s.draft.publicReplies.map((r, i) =>
        i === index ? { ...r, ...patch } : r);
      return { draft: { ...s.draft, publicReplies: next } };
    }),

  removePublicReply: (index) =>
    set((s) => ({
      draft: {
        ...s.draft,
        publicReplies: s.draft.publicReplies.filter((_, i) => i !== index),
      },
    })),

  setFollowGateEnabled: (followGateEnabled) =>
    set((s) => ({ draft: { ...s.draft, followGateEnabled } })),

  setFollowGateMessage: (followGateMessage) =>
    set((s) => ({ draft: { ...s.draft, followGateMessage } })),

  setFollowGateButtonLabel: (followGateButtonLabel) =>
    set((s) => ({ draft: { ...s.draft, followGateButtonLabel } })),

  setBotProtectionEnabled: (botProtectionEnabled) =>
    set((s) => ({ draft: { ...s.draft, botProtectionEnabled } })),

  setBotProtectionMinDelay: (botProtectionMinDelaySeconds) =>
    set((s) => ({ draft: { ...s.draft, botProtectionMinDelaySeconds } })),

  setBotProtectionMaxDelay: (botProtectionMaxDelaySeconds) =>
    set((s) => ({ draft: { ...s.draft, botProtectionMaxDelaySeconds } })),

  setFollowUpEnabled: (followUpEnabled) =>
    set((s) => ({ draft: { ...s.draft, followUpEnabled } })),

  setFollowUpDelayAmount: (followUpDelayAmount) =>
    set((s) => ({ draft: { ...s.draft, followUpDelayAmount } })),

  setFollowUpDelayUnit: (followUpDelayUnit) =>
    set((s) => ({ draft: { ...s.draft, followUpDelayUnit } })),

  setFollowUpMessage: (followUpMessage) =>
    set((s) => ({ draft: { ...s.draft, followUpMessage } })),

  setActionType: (type) =>
    set((s) => ({ draft: { ...s.draft, action: { ...s.draft.action, type } } })),

  setActionLink: (link) =>
    set((s) => ({ draft: { ...s.draft, action: { ...s.draft.action, link } } })),

  setMessage: (message) =>
    set((s) => ({ draft: { ...s.draft, message } })),

  addAction: (type = ACTION_TYPE.SEND_MESSAGE) =>
    set((s) => ({
      draft: { ...s.draft, actions: [...s.draft.actions, blankAction(type)] },
    })),

  updateAction: (index, patch) =>
    set((s) => {
      const next = s.draft.actions.map((a, i) =>
        i === index ? { ...a, ...patch } : a);
      return { draft: { ...s.draft, actions: next } };
    }),

  removeAction: (index) =>
    set((s) => {
      if (s.draft.actions.length <= 1) return s;
      const next = s.draft.actions.filter((_, i) => i !== index);
      return { draft: { ...s.draft, actions: next } };
    }),

  duplicateAction: (index) =>
    set((s) => {
      const source = s.draft.actions[index];
      if (!source) return s;
      const copy = { ...source };
      const next = [
        ...s.draft.actions.slice(0, index + 1),
        copy,
        ...s.draft.actions.slice(index + 1),
      ];
      return { draft: { ...s.draft, actions: next } };
    }),

  moveAction: (index, delta) =>
    set((s) => {
      const newIndex = index + delta;
      if (newIndex < 0 || newIndex >= s.draft.actions.length) return s;
      const next = [...s.draft.actions];
      const [item] = next.splice(index, 1);
      next.splice(newIndex, 0, item);
      return { draft: { ...s.draft, actions: next } };
    }),
}));