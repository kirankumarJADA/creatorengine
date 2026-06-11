import { create } from 'zustand';
import {
  TRIGGER_TYPE,
  CONDITION_TYPE,
  MATCH_TYPE,
  ACTION_TYPE,
} from '../utils/constants.js';

/**
 * Builder draft store — owns the in-flight wizard state.
 */

export const STEPS = Object.freeze([
  { id: 1, key: 'trigger',   label: 'Trigger' },
  { id: 2, key: 'condition', label: 'Condition' },
  { id: 3, key: 'action',    label: 'Actions' },
  { id: 4, key: 'message',   label: 'Preview' },
  { id: 5, key: 'review',    label: 'Review' },
]);

/** A fresh action card with sensible defaults for the picker. */
export const blankAction = (type = ACTION_TYPE.SEND_MESSAGE) => ({
  type,
  message: '',
  link: '',
  delaySeconds: type === ACTION_TYPE.DELAY ? 5 : null,
});

const emptyDraft = () => ({
  name: '',
  trigger: null,
  targetPostId: null,
  condition: {
    type: CONDITION_TYPE.ANY,
    keyword: '',
    matchType: MATCH_TYPE.CONTAINS,
  },
  action: { type: ACTION_TYPE.SEND_DM, link: '' },
  message: '',
  actions: [blankAction(ACTION_TYPE.SEND_MESSAGE)],
  // Public comment reply.
  publicReplyEnabled: false,
  publicReplies: [],
  // Follow gate.
  followGateEnabled: false,
  followGateMessage: '',
  followGateButtonLabel: '',
  enabled: true,
});

const normalizeActionsFromBackend = (automation) => {
  if (Array.isArray(automation.actions) && automation.actions.length > 0) {
    return automation.actions.map((a) => ({
      type:         a.type ?? ACTION_TYPE.SEND_MESSAGE,
      message:      a.message ?? '',
      link:         a.link ?? '',
      delaySeconds: a.delaySeconds ?? null,
    }));
  }
  if (automation.action) {
    return [{
      type:         automation.action.type ?? ACTION_TYPE.SEND_DM,
      message:      automation.message ?? '',
      link:         automation.action.link ?? '',
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

export const useBuilderStore = create((set, get) => ({
  // ─── State ─────────────────────────────────────
  step: 1,
  mode: 'create',
  editingId: null,
  draft: emptyDraft(),

  // ─── Lifecycle ─────────────────────────────────
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
      enabled: automation.enabled !== false,
    },
  }),

  reset: () => set({ step: 1, mode: 'create', editingId: null, draft: emptyDraft() }),

  // ─── Navigation ────────────────────────────────
  goToStep: (step) => {
    const clamped = Math.max(1, Math.min(STEPS.length, step));
    set({ step: clamped });
  },
  next: () => set((s) => ({ step: Math.min(STEPS.length, s.step + 1) })),
  prev: () => set((s) => ({ step: Math.max(1, s.step - 1) })),

  // ─── Top-level mutations ───────────────────────
  setTrigger: (trigger) =>
    set((s) => ({ draft: { ...s.draft, trigger } })),

  setTargetPostId: (targetPostId) =>
    set((s) => ({ draft: { ...s.draft, targetPostId } })),

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

  // ─── Public reply mutations ────────────────────
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

  // ─── Follow gate mutations ─────────────────────
  setFollowGateEnabled: (followGateEnabled) =>
    set((s) => ({ draft: { ...s.draft, followGateEnabled } })),

  setFollowGateMessage: (followGateMessage) =>
    set((s) => ({ draft: { ...s.draft, followGateMessage } })),

  setFollowGateButtonLabel: (followGateButtonLabel) =>
    set((s) => ({ draft: { ...s.draft, followGateButtonLabel } })),

  // ─── Legacy single-action mutations ────────────
  setActionType: (type) =>
    set((s) => ({ draft: { ...s.draft, action: { ...s.draft.action, type } } })),

  setActionLink: (link) =>
    set((s) => ({ draft: { ...s.draft, action: { ...s.draft.action, link } } })),

  setMessage: (message) =>
    set((s) => ({ draft: { ...s.draft, message } })),

  // ─── Multi-action mutations ────────────────────
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