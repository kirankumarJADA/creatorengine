import {
  TRIGGER_TYPE,
  CONDITION_TYPE,
  MATCH_TYPE,
  ACTION_TYPE,
  CONTACT_STATUS,
} from './constants.js';

/**
 * Mock data for the UI build-out.
 *
 * Automations follow the same schema as the backend's
 * {@code Automation} entity — keeping them aligned means the same
 * components render mock and real data without branching.
 */

// ─── Dashboard stats ──────────────────────────────────────────
export const mockStats = {
  totalAutomations: 12,
  activeAutomations: 8,
  totalContacts: 1_284,
  messagesSent: 9_472,
  deltas: {
    totalAutomations: '+2 this week',
    activeAutomations: '+1 today',
    totalContacts: '+86 this week',
    messagesSent: '+12.4%',
  },
};

// ─── Automations ──────────────────────────────────────────────
export const mockAutomations = [
  {
    id: 'auto_01',
    name: 'Link drop on giveaway reel',
    trigger: TRIGGER_TYPE.COMMENT,
    condition: {
      type: CONDITION_TYPE.KEYWORD,
      keyword: 'link',
      matchType: MATCH_TYPE.CONTAINS,
    },
    action: { type: ACTION_TYPE.SEND_DM, link: null },
    message: 'Hey {{username}} 👋 here is the link you asked for: https://yourlink.com',
    enabled: true,
    runCount: 312,
    successCount: 308,
    createdAt: '2026-04-22T08:10:00Z',
    updatedAt: '2026-05-18T11:23:00Z',
  },
  {
    id: 'auto_02',
    name: 'Welcome new followers',
    trigger: TRIGGER_TYPE.DM,
    condition: { type: CONDITION_TYPE.ANY, keyword: null, matchType: null },
    action: { type: ACTION_TYPE.SEND_MESSAGE, link: null },
    message: 'Hey {{username}}! Welcome aboard — what brought you here?',
    enabled: true,
    runCount: 1_204,
    successCount: 1_188,
    createdAt: '2026-03-11T12:00:00Z',
    updatedAt: '2026-05-17T08:02:00Z',
  },
  {
    id: 'auto_03',
    name: 'Story mention thank-you',
    trigger: TRIGGER_TYPE.STORY_REPLY,
    condition: { type: CONDITION_TYPE.ANY, keyword: null, matchType: null },
    action: { type: ACTION_TYPE.SEND_DM, link: null },
    message: 'Thanks so much for the share, {{username}} ❤️',
    enabled: false,
    runCount: 89,
    successCount: 81,
    createdAt: '2026-04-05T18:30:00Z',
    updatedAt: '2026-05-12T19:50:00Z',
  },
  {
    id: 'auto_04',
    name: 'Course waitlist DM',
    trigger: TRIGGER_TYPE.COMMENT,
    condition: {
      type: CONDITION_TYPE.KEYWORD,
      keyword: 'course',
      matchType: MATCH_TYPE.CONTAINS,
    },
    action: {
      type: ACTION_TYPE.SEND_LINK,
      link: 'https://yourlink.com/course',
    },
    message: 'You\'re in, {{username}}! Join the waitlist here: https://yourlink.com/course',
    enabled: true,
    runCount: 47,
    successCount: 47,
    createdAt: '2026-05-01T09:00:00Z',
    updatedAt: '2026-05-19T14:15:00Z',
  },
  {
    id: 'auto_05',
    name: 'Affiliate link auto-share',
    trigger: TRIGGER_TYPE.COMMENT,
    condition: {
      type: CONDITION_TYPE.KEYWORD,
      keyword: 'shop',
      matchType: MATCH_TYPE.EXACT,
    },
    action: { type: ACTION_TYPE.SEND_LINK, link: 'https://yourlink.com/shop' },
    message: 'Here\'s the shop link, {{username}} 🛍️ https://yourlink.com/shop',
    enabled: true,
    runCount: 522,
    successCount: 514,
    createdAt: '2026-04-18T11:45:00Z',
    updatedAt: '2026-05-16T12:30:00Z',
  },
  {
    id: 'auto_06',
    name: 'Save VIP commenters',
    trigger: TRIGGER_TYPE.COMMENT,
    condition: {
      type: CONDITION_TYPE.KEYWORD,
      keyword: 'vip',
      matchType: MATCH_TYPE.CONTAINS,
    },
    action: { type: ACTION_TYPE.SAVE_CONTACT, link: null },
    message: 'Welcome to the VIP list, {{username}}!',
    enabled: true,
    runCount: 18,
    successCount: 18,
    createdAt: '2026-05-10T13:20:00Z',
    updatedAt: '2026-05-15T10:00:00Z',
  },
];

// ─── Contacts ─────────────────────────────────────────────────
export const mockContacts = [
  { id: 'c_01', name: 'Aria Patel',      username: 'aria.patel',      source: 'Reel Comment',  date: '2026-05-19T15:30:00Z', status: CONTACT_STATUS.SUBSCRIBED },
  { id: 'c_02', name: 'Marcus Chen',     username: 'marcuschen',      source: 'Story Mention', date: '2026-05-19T11:12:00Z', status: CONTACT_STATUS.SUBSCRIBED },
  { id: 'c_03', name: 'Sofia Rodríguez', username: 'sofi.r',          source: 'New Follower',  date: '2026-05-18T22:48:00Z', status: CONTACT_STATUS.SUBSCRIBED },
  { id: 'c_04', name: 'Theo Hill',       username: 'theohill',        source: 'Post Comment',  date: '2026-05-18T09:05:00Z', status: CONTACT_STATUS.UNSUBSCRIBED },
  { id: 'c_05', name: 'Maya Singh',      username: 'mayasingh_',      source: 'Reel Comment',  date: '2026-05-17T19:22:00Z', status: CONTACT_STATUS.SUBSCRIBED },
  { id: 'c_06', name: 'Jordan Wright',   username: 'jwrightofficial', source: 'Reel Comment',  date: '2026-05-17T14:40:00Z', status: CONTACT_STATUS.BOUNCED },
  { id: 'c_07', name: 'Nia Okafor',      username: 'niaokafor',       source: 'Story Mention', date: '2026-05-17T08:18:00Z', status: CONTACT_STATUS.SUBSCRIBED },
  { id: 'c_08', name: 'Luca Bianchi',    username: 'luca.b',          source: 'New Follower',  date: '2026-05-16T20:10:00Z', status: CONTACT_STATUS.SUBSCRIBED },
  { id: 'c_09', name: 'Priya Kapoor',    username: 'priyakapoor',     source: 'Post Comment',  date: '2026-05-16T16:55:00Z', status: CONTACT_STATUS.SUBSCRIBED },
  { id: 'c_10', name: 'Ethan Walker',    username: 'ethanw',          source: 'Reel Comment',  date: '2026-05-16T10:30:00Z', status: CONTACT_STATUS.SUBSCRIBED },
];

// ─── Recent activity feed ─────────────────────────────────────
export const mockActivity = [
  { id: 'act_01', type: 'message_sent',         message: 'DM sent to @aria.patel from "Link drop on giveaway reel"', timeAgo: '3m ago' },
  { id: 'act_02', type: 'contact_added',        message: 'New contact: @marcuschen via Story Mention',               timeAgo: '12m ago' },
  { id: 'act_03', type: 'automation_triggered', message: '"Welcome new followers" triggered 4 times',                timeAgo: '24m ago' },
  { id: 'act_04', type: 'message_sent',         message: 'DM sent to @sofi.r from "Welcome new followers"',          timeAgo: '38m ago' },
  { id: 'act_05', type: 'automation_paused',    message: '"Story mention thank-you" paused by you',                  timeAgo: '2h ago' },
  { id: 'act_06', type: 'message_sent',         message: 'DM sent to @theohill from "Course waitlist DM"',           timeAgo: '3h ago' },
  { id: 'act_07', type: 'contact_added',        message: 'New contact: @niaokafor via Story Mention',                timeAgo: '5h ago' },
];
