/**
 * Centralised constants. Importing from one place avoids
 * "stringly-typed" bugs and makes refactors safe.
 */

export const ROUTES = Object.freeze({
  // Public
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',

  // Authenticated
  DASHBOARD: '/dashboard',
  AUTOMATIONS: '/automations',
  AUTOMATION_NEW: '/automations/new',
  AUTOMATION_EDIT: '/automations/:id/edit',
  CONTACTS: '/contacts',
  SETTINGS: '/settings',
  LOGS:     '/logs',
  FAILED_JOBS: '/failed-jobs',

  // Instagram OAuth bounce-back page
  INSTAGRAM_CALLBACK: '/instagram/callback',
});

/** Helper for parameterised routes (use instead of string templating). */
export const buildRoute = {
  automationEdit: (id) => `/automations/${id}/edit`,
};

export const STORAGE_KEYS = Object.freeze({
  ACCESS_TOKEN:  'ce.access_token',
  REFRESH_TOKEN: 'ce.refresh_token',
  USER:          'ce.user',
  THEME:         'ce.theme',
  SIDEBAR_OPEN:  'ce.sidebar_open',
});

export const API_ENDPOINTS = Object.freeze({
  REGISTER:        '/auth/register',
  LOGIN:           '/auth/login',
  LOGOUT:          '/auth/logout',
  ME:              '/auth/me',
  FORGOT_PASSWORD: '/auth/forgot-password',

  AUTOMATIONS:      '/automations',
  AUTOMATION_BY_ID: (id) => `/automations/${id}`,
  AUTOMATION_TOGGLE: (id) => `/automations/${id}/toggle`,

  IG_CONNECT:    '/instagram/connect',
  IG_DISCONNECT: '/instagram/disconnect',
  IG_STATUS:     '/instagram/status',

  CONTACTS:      '/contacts',
  LOGS:          '/logs',
  FAILED_JOBS:           '/failed-jobs',
  FAILED_JOB_BY_ID:      (id) => `/failed-jobs/${id}`,
  FAILED_JOB_RETRY:      (id) => `/failed-jobs/${id}/retry`,

  /** AI message assistant — returns 3 DM template suggestions. */
  AI_GENERATE_MESSAGE: '/ai/generate-message',
});

/* ─────────────────────────────────────────────
   Activity log status — mirrors backend
   com.creatorengine.automation.logger.ExecutionLogger.STATUS_*.
   ───────────────────────────────────────────── */
export const LOG_STATUS = Object.freeze({
  SUCCESS:           'SUCCESS',
  FAILED:            'FAILED',
  COOLDOWN_SKIPPED:  'COOLDOWN_SKIPPED',
  DUPLICATE_IGNORED: 'DUPLICATE_IGNORED',
});

/* ─────────────────────────────────────────────
   Instagram connection state — mirrors backend
   com.creatorengine.instagram.entity.ConnectionStatus.
   ───────────────────────────────────────────── */
export const CONNECTION_STATUS = Object.freeze({
  NOT_CONNECTED: 'NOT_CONNECTED',
  CONNECTED:     'CONNECTED',
  EXPIRED:       'EXPIRED',
});

/* ─────────────────────────────────────────────
   Automation domain enums — kept in lockstep with the
   backend enums under com.creatorengine.automation.entity.
   ───────────────────────────────────────────── */

export const TRIGGER_TYPE = Object.freeze({
  COMMENT:      'COMMENT',
  DM:           'DM',
  STORY_REPLY:  'STORY_REPLY',
});

export const TRIGGER_LABEL = Object.freeze({
  [TRIGGER_TYPE.COMMENT]:     'Comment on Post/Reel',
  [TRIGGER_TYPE.DM]:          'DM Message',
  [TRIGGER_TYPE.STORY_REPLY]: 'Story Reply',
});

export const CONDITION_TYPE = Object.freeze({
  ANY:     'ANY',
  KEYWORD: 'KEYWORD',
});

export const MATCH_TYPE = Object.freeze({
  EXACT:    'EXACT',
  CONTAINS: 'CONTAINS',
});

export const ACTION_TYPE = Object.freeze({
  SEND_DM:       'SEND_DM',
  SEND_MESSAGE:  'SEND_MESSAGE',
  SEND_LINK:     'SEND_LINK',
  SAVE_CONTACT:  'SAVE_CONTACT',
  DELAY:         'DELAY',
});

export const ACTION_LABEL = Object.freeze({
  [ACTION_TYPE.SEND_DM]:      'Send DM',
  [ACTION_TYPE.SEND_MESSAGE]: 'Send Message',
  [ACTION_TYPE.SEND_LINK]:    'Send Link',
  [ACTION_TYPE.SAVE_CONTACT]: 'Save Contact',
  [ACTION_TYPE.DELAY]:        'Delay',
});

/** UI-only — the wire format always stores delaySeconds. */
export const DELAY_UNIT = Object.freeze({
  SECONDS: 'seconds',
  MINUTES: 'minutes',
});

/** Inclusive caps; mirror the backend AutomationRequest validators. */
export const DELAY_MIN_SECONDS = 1;
export const DELAY_MAX_SECONDS = 24 * 60 * 60;

/** AI message tone — matches backend MessageTone enum exactly. */
export const MESSAGE_TONE = Object.freeze({
  FRIENDLY:     'FRIENDLY',
  PROFESSIONAL: 'PROFESSIONAL',
  SALES:        'SALES',
  CASUAL:       'CASUAL',
});

export const MESSAGE_TONE_LABEL = Object.freeze({
  [MESSAGE_TONE.FRIENDLY]:     'Friendly',
  [MESSAGE_TONE.PROFESSIONAL]: 'Professional',
  [MESSAGE_TONE.SALES]:        'Sales',
  [MESSAGE_TONE.CASUAL]:       'Casual',
});

/* Kept around for the Contacts page. */
export const CONTACT_STATUS = Object.freeze({
  SUBSCRIBED:   'SUBSCRIBED',
  UNSUBSCRIBED: 'UNSUBSCRIBED',
  BOUNCED:      'BOUNCED',
});

export const APP_NAME = import.meta.env.VITE_APP_NAME || 'CreatorEngine';

/** Pagination defaults for the automations list. */
export const PAGE_SIZE = 9;
