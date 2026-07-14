import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

const instagramService = {
  /** Get the first connected account status (legacy / single-account) */
  getStatus: async () => {
    const { data } = await api.get(API_ENDPOINTS.IG_STATUS);
    return data;
  },

  /** Get ALL connected Instagram accounts for the current user */
  getAccounts: async () => {
    const { data } = await api.get('/instagram/accounts');
    return Array.isArray(data) ? data : [];
  },

  startConnect: async () => {
    const { data } = await api.get(API_ENDPOINTS.IG_CONNECT);
    return data;
  },

  /** Disconnect a specific account by instagramUserId */
  disconnect: async (igAccountId) => {
    const params = igAccountId ? { igAccountId } : {};
    await api.post(API_ENDPOINTS.IG_DISCONNECT, null, { params });
  },

  /** Get media for a specific account (or the first connected one) */
  getMedia: async (igAccountId) => {
    const params = igAccountId ? { igAccountId } : {};
    const { data } = await api.get('/instagram/media', { params });
    return data;
  },

  /** Check if the user can connect another account based on their plan */
  getPlanLimits: async () => {
    const { data } = await api.get('/instagram/plan-limits');
    return data; // { canAdd, currentAccounts, maxAccounts, plan }
  },
};

export default instagramService;