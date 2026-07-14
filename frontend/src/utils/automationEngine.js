import {
  TRIGGER_TYPE,
  CONDITION_TYPE,
  MATCH_TYPE,
  ACTION_TYPE,
  TRIGGER_LABEL,
  ACTION_LABEL,
} from './constants.js';

// ─── Template variables ───────────────────────────────────────
const VARIABLE_PATTERN = /\{\{\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*\}\}/g;

export const renderTemplate = (template = '', vars = {}) => {
  if (!template) return '';
  return template.replace(VARIABLE_PATTERN, (full, name) => {
    return Object.prototype.hasOwnProperty.call(vars, name)
      ? String(vars[name])
      : full;
  });
};

export const extractVariables = (template = '') => {
  const set = new Set();
  let m;
  while ((m = VARIABLE_PATTERN.exec(template)) !== null) {
    set.add(m[1]);
  }
  return Array.from(set);
};

// ─── Matching ─────────────────────────────────────────────────

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

export const simulateRun = (automation, event) => {
  const { matched, reason } = evaluateAutomation(automation, event);
  if (!matched) {
    return { matched: false, reason, output: null };
  }

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
      steps,
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

  // The action chain is now OPTIONAL. An automation can do just a
  // public reply (no DM) — useful for viral comment hype like "follow
  // for more!" — so we no longer require at least one effective action.
  // Per-action validation still runs when the user did add actions.
  const actions = Array.isArray(draft.actions) ? draft.actions : [];
  if (actions.length > 0) {
    const perAction = actions.map((a) => validateOneAction(a));
    if (perAction.some(Boolean)) {
      errors.actions = perAction;
    }
  }

  return { isValid: Object.keys(errors).length === 0, errors };
};

const validateOneAction = (action) => {
  // Empty/blank steps are tolerated — AutomationBuilder strips them
  // before save, so the backend never sees no-op rows.
  if (!action || !action.type) return null;
  if (!Object.values(ACTION_TYPE).includes(action.type)) return 'Unknown action type.';

  switch (action.type) {
    case ACTION_TYPE.SEND_LINK:
      // The link is the whole point of SEND_LINK, so it's still required.
      if (!action.link || action.link.trim().length === 0) return 'Enter a link.';
      if (action.message && action.message.length > 2000) {
        return 'Message is too long (max 2000 characters).';
      }
      return null;

    case ACTION_TYPE.SEND_MESSAGE:
    case ACTION_TYPE.SEND_DM:
      // Message no longer required — supports "public reply only" automations.
      if (action.message && action.message.length > 2000) {
        return 'Message is too long (max 2000 characters).';
      }
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