import {
  TRIGGER_TYPE,
  CONDITION_TYPE,
  MATCH_TYPE,
  ACTION_TYPE,
  TRIGGER_LABEL,
  ACTION_LABEL,
} from './constants.js';

/**
 * Pure-function "mini engine" that mirrors what the real backend
 * webhook handler will do when an Instagram event arrives.
 *
 * Used in two places:
 *  1. The builder's message preview — substitutes {{username}} so the
 *     user sees a realistic DM bubble while editing.
 *  2. The simulator modal — lets the user fire a fake event against
 *     a saved automation and verify the matching + rendering logic
 *     before any real webhook is wired up.
 *
 * Keeping these as pure functions (no React, no fetch) means tests
 * can hit them directly, and the same code can later be lifted to a
 * Node/Spring port without modification.
 */

// ─── Template variables ───────────────────────────────────────
const VARIABLE_PATTERN = /\{\{\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*\}\}/g;

/**
 * Replace {{username}}-style placeholders with values from `vars`.
 * Unknown variables are left intact so the user can spot typos.
 */
export const renderTemplate = (template = '', vars = {}) => {
  if (!template) return '';
  return template.replace(VARIABLE_PATTERN, (full, name) => {
    return Object.prototype.hasOwnProperty.call(vars, name)
      ? String(vars[name])
      : full;
  });
};

/** Extract the variable names referenced in a template. */
export const extractVariables = (template = '') => {
  const set = new Set();
  let m;
  while ((m = VARIABLE_PATTERN.exec(template)) !== null) {
    set.add(m[1]);
  }
  return Array.from(set);
};

// ─── Matching ─────────────────────────────────────────────────

/**
 * Test whether an automation should fire for a given event.
 *
 * @param {object} automation - the saved automation
 * @param {object} event - { type: TRIGGER_TYPE, content: string, username: string }
 * @returns {{ matched: boolean, reason: string }}
 */
export const evaluateAutomation = (automation, event) => {
  if (!automation || !event) {
    return { matched: false, reason: 'No automation or event supplied.' };
  }
  if (!automation.enabled) {
    return { matched: false, reason: 'Automation is paused.' };
  }
  if (automation.trigger !== event.type) {
    return {
      matched: false,
      reason: `Trigger mismatch: automation expects "${TRIGGER_LABEL[automation.trigger]}" but event was "${TRIGGER_LABEL[event.type]}".`,
    };
  }

  const cond = automation.condition || { type: CONDITION_TYPE.ANY };
  if (cond.type === CONDITION_TYPE.ANY) {
    return { matched: true, reason: 'Any incoming event matches.' };
  }

  if (cond.type === CONDITION_TYPE.KEYWORD) {
    const haystack = (event.content || '').toLowerCase().trim();
    const needle = (cond.keyword || '').toLowerCase().trim();
    if (!needle) {
      return { matched: false, reason: 'No keyword configured.' };
    }
    if (cond.matchType === MATCH_TYPE.EXACT) {
      const ok = haystack === needle;
      return {
        matched: ok,
        reason: ok
          ? `Exact keyword match: "${needle}".`
          : `Exact match required — "${event.content}" ≠ "${needle}".`,
      };
    }
    // CONTAINS
    const ok = haystack.includes(needle);
    return {
      matched: ok,
      reason: ok
        ? `Keyword "${needle}" found in content.`
        : `Content does not contain "${needle}".`,
    };
  }

  return { matched: false, reason: 'Unknown condition type.' };
};

/**
 * Run an automation against an event and return what would happen.
 *
 * Mirrors what the backend will do — useful for the simulator.
 */
export const simulateRun = (automation, event) => {
  const { matched, reason } = evaluateAutomation(automation, event);
  if (!matched) {
    return { matched: false, reason, output: null };
  }

  // Resolve the effective chain — same logic as the backend's
  // Automation.getEffectiveActions(): prefer actions[], else wrap the
  // legacy single action + top-level message.
  const chain = Array.isArray(automation.actions) && automation.actions.length > 0
    ? automation.actions
    : (automation.action
        ? [{
            type:    automation.action.type ?? ACTION_TYPE.SEND_DM,
            message: automation.message,
            link:    automation.action.link,
          }]
        : []);

  const vars = { username: event.username || 'follower' };

  // Build per-step preview info so the modal (or any future richer
  // simulator) can render the full chain.
  const steps = chain.map((action, i) => {
    const a = action || {};
    switch (a.type) {
      case ACTION_TYPE.DELAY: {
        const secs = Number(a.delaySeconds) || 0;
        return {
          index: i,
          type:  ACTION_TYPE.DELAY,
          label: ACTION_LABEL[ACTION_TYPE.DELAY],
          kind:  `Pause for ${secs} second${secs === 1 ? '' : 's'}.`,
        };
      }
      case ACTION_TYPE.SAVE_CONTACT:
        return {
          index: i,
          type:  ACTION_TYPE.SAVE_CONTACT,
          label: ACTION_LABEL[ACTION_TYPE.SAVE_CONTACT],
          kind:  'The contact would be saved.',
        };
      case ACTION_TYPE.SEND_LINK: {
        const msg = renderTemplate(a.message, vars);
        return {
          index:   i,
          type:    ACTION_TYPE.SEND_LINK,
          label:   ACTION_LABEL[ACTION_TYPE.SEND_LINK],
          kind:    'A DM with the link would be sent.',
          message: msg,
          link:    a.link || null,
        };
      }
      case ACTION_TYPE.SEND_MESSAGE:
      case ACTION_TYPE.SEND_DM: {
        const msg = renderTemplate(a.message, vars);
        return {
          index:   i,
          type:    a.type,
          label:   ACTION_LABEL[a.type],
          kind:    a.type === ACTION_TYPE.SEND_DM
                   ? 'A DM would be sent.'
                   : 'A message would be sent.',
          message: msg,
        };
      }
      default:
        return {
          index: i,
          type:  a.type,
          label: ACTION_LABEL[a.type] || a.type,
          kind:  'An action would be performed.',
        };
    }
  });

  // Headline action for the modal's current single-preview UI: the
  // first send-type step in the chain. The legacy `output` shape stays
  // intact so SimulatorModal keeps working — it just gets richer data.
  const primary = steps.find(
    (s) => s.type === ACTION_TYPE.SEND_MESSAGE
        || s.type === ACTION_TYPE.SEND_DM
        || s.type === ACTION_TYPE.SEND_LINK
  ) || steps[0] || null;

  return {
    matched: true,
    reason,
    output: primary ? {
      kind:    chain.length > 1
               ? `${primary.kind} (chain has ${chain.length} steps)`
               : primary.kind,
      action:  primary.label || '—',
      message: primary.message || '',
      link:    primary.link || null,
      to:      event.username || 'follower',
      steps,                                 // full chain for future renderers
    } : null,
  };
};

// ─── Validation (used by the builder before save) ─────────────

export const validateAutomationDraft = (draft = {}) => {
  const errors = {};

  if (!draft.trigger || !Object.values(TRIGGER_TYPE).includes(draft.trigger)) {
    errors.trigger = 'Pick a trigger.';
  }

  const cond = draft.condition || {};
  if (!cond.type || !Object.values(CONDITION_TYPE).includes(cond.type)) {
    errors.condition = 'Pick a condition.';
  } else if (cond.type === CONDITION_TYPE.KEYWORD) {
    if (!cond.keyword || cond.keyword.trim().length === 0) {
      errors.keyword = 'Enter a keyword to match.';
    }
    if (!cond.matchType || !Object.values(MATCH_TYPE).includes(cond.matchType)) {
      errors.matchType = 'Pick how the keyword should match.';
    }
  }

  // ─── Multi-action chain validation ─────────────────────
  const actions = Array.isArray(draft.actions) ? draft.actions : [];

  if (actions.length === 0) {
    errors.actionsChain = 'Add at least one step.';
  } else {
    const anyEffective = actions.some((a) => a && a.type !== ACTION_TYPE.DELAY);
    if (!anyEffective) {
      errors.actionsChain = 'At least one step must do something other than DELAY.';
    }

    // Per-action errors, indexed positionally so the ActionStep can
    // render them next to the offending card.
    const perAction = actions.map((a) => validateOneAction(a));
    if (perAction.some(Boolean)) {
      errors.actions = perAction;
    }
  }

  return { isValid: Object.keys(errors).length === 0, errors };
};

const validateOneAction = (action) => {
  if (!action || !action.type) return 'Pick an action type.';
  if (!Object.values(ACTION_TYPE).includes(action.type)) return 'Unknown action type.';

  switch (action.type) {
    case ACTION_TYPE.SEND_LINK:
      if (!action.message || action.message.trim().length === 0) return 'Enter a message.';
      if (!action.link || action.link.trim().length === 0) return 'Enter a link.';
      if (action.message.length > 2000) return 'Message is too long (max 2000 characters).';
      return null;

    case ACTION_TYPE.SEND_MESSAGE:
    case ACTION_TYPE.SEND_DM:
      if (!action.message || action.message.trim().length === 0) return 'Enter a message.';
      if (action.message.length > 2000) return 'Message is too long (max 2000 characters).';
      return null;

    case ACTION_TYPE.DELAY: {
      const secs = Number(action.delaySeconds);
      if (!Number.isFinite(secs) || secs < 1) return 'Delay must be at least 1 second.';
      if (secs > 24 * 60 * 60) return 'Delay can be at most 24 hours.';
      return null;
    }

    case ACTION_TYPE.SAVE_CONTACT:
      return null;

    default:
      return null;
  }
};
