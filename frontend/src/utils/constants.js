export const ROUTES = Object.freeze({
  LOGIN: '/login',
  GOOGLE_SIGN_IN: '/auth/google',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',
  RESET_PASSWORD: '/reset-password',

  DASHBOARD: '/dashboard',
  AUTOMATIONS: '/automations',
  AUTOMATION_NEW: '/automations/new',
  AUTOMATION_EDIT: '/automations/:id/edit',
  CONTACTS: '/contacts',
  SETTINGS: '/settings',
  LOGS:     '/logs',
  FAILED_JOBS: '/failed-jobs',

  ADMIN:              '/admin',
  ADMIN_USERS:        '/admin/users',
  ADMIN_AUTOMATIONS:  '/admin/automations',
  ADMIN_LOGS:         '/admin/logs',
  ADMIN_FAILED_JOBS:  '/admin/failed-jobs',
  ADMIN_SYSTEM:       '/admin/system',

  INSTAGRAM_CALLBACK: '/instagram/callback',
});

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
  SEND_OTP:        '/auth/send-otp',
  VERIFY_OTP:      '/auth/verify-otp',
  CONFIRM_RESET:   '/auth/confirm-reset',
  GOOGLE_SIGN_IN:  '/auth/google',
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
  ADMIN_DASHBOARD:        '/admin/dashboard',
  ADMIN_USERS:            '/admin/users',
  ADMIN_USER_BY_ID:       (uid) => `/admin/users/${uid}`,
  ADMIN_USER_ENABLE:      (uid) => `/admin/users/${uid}/enable`,
  ADMIN_USER_DISABLE:     (uid) => `/admin/users/${uid}/disable`,

  ADMIN_AUTOMATIONS:        '/admin/automations',
  ADMIN_AUTOMATION_BY_ID:   (uid, id) => `/admin/automations/${uid}/${id}`,
  ADMIN_AUTOMATION_TOGGLE:  (uid, id) => `/admin/automations/${uid}/${id}/toggle`,

  ADMIN_LOGS: '/admin/logs',

  ADMIN_FAILED_JOBS:       '/admin/failed-jobs',
  ADMIN_FAILED_JOB_BY_ID:  (uid, id) => `/admin/failed-jobs/${uid}/${id}`,
  ADMIN_FAILED_JOB_RETRY:  (uid, id) => `/admin/failed-jobs/${uid}/${id}/retry`,

  ADMIN_SYSTEM: '/admin/system',

  AI_GENERATE_MESSAGE: '/ai/generate-message',
});

export const LOG_STATUS = Object.freeze({
  SUCCESS:           'SUCCESS',
  FAILED:            'FAILED',
  COOLDOWN_SKIPPED:  'COOLDOWN_SKIPPED',
  DUPLICATE_IGNORED: 'DUPLICATE_IGNORED',
});

export const CONNECTION_STATUS = Object.freeze({
  NOT_CONNECTED: 'NOT_CONNECTED',
  CONNECTED:     'CONNECTED',
  EXPIRED:       'EXPIRED',
});

export const TRIGGER_TYPE = Object.freeze({
  COMMENT:        'COMMENT',
  DM:             'DM',
  STORY_REPLY:    'STORY_REPLY',
  NEXT_POST:      'NEXT_POST',
  CONTENT_SHARED: 'CONTENT_SHARED',
  LIVE_COMMENT:   'LIVE_COMMENT',
});

export const TRIGGER_LABEL = Object.freeze({
  [TRIGGER_TYPE.COMMENT]:        'Comment on Post/Reel',
  [TRIGGER_TYPE.DM]:             'DM Message',
  [TRIGGER_TYPE.STORY_REPLY]:    'Story Reply',
  [TRIGGER_TYPE.NEXT_POST]:      'Next Post',
  [TRIGGER_TYPE.CONTENT_SHARED]: 'Content Shared in DM',
  [TRIGGER_TYPE.LIVE_COMMENT]:   'Live Comment',
});

export const POST_TARGET_MODE = Object.freeze({
  ALL:       'ALL',
  SPECIFIC:  'SPECIFIC',
  NEXT_POST: 'NEXT_POST',
});

export const POST_TARGET_MODE_LABEL = Object.freeze({
  [POST_TARGET_MODE.ALL]:       'All posts',
  [POST_TARGET_MODE.SPECIFIC]:  'Specific post',
  [POST_TARGET_MODE.NEXT_POST]: 'Next post',
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

export const DELAY_UNIT = Object.freeze({
  SECONDS: 'seconds',
  MINUTES: 'minutes',
});

export const DELAY_MIN_SECONDS = 1;
export const DELAY_MAX_SECONDS = 24 * 60 * 60;

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

export const CONTACT_STATUS = Object.freeze({
  SUBSCRIBED:   'SUBSCRIBED',
  UNSUBSCRIBED: 'UNSUBSCRIBED',
  BOUNCED:      'BOUNCED',
});

export const APP_NAME = import.meta.env.VITE_APP_NAME || 'CreatorEngine';

export const PAGE_SIZE = 9;