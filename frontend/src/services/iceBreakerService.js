import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

const iceBreakerService = {
  /** Fetch current ice breakers from Instagram */
  getAll: async () => {
    const { data } = await api.get(API_ENDPOINTS.ICE_BREAKERS);
    return Array.isArray(data) ? data : [];
  },

  /** Save ice breakers to Instagram (replaces all existing) */
  save: async (questions) => {
    await api.put(API_ENDPOINTS.ICE_BREAKERS, questions);
  },

  /** Clear all ice breakers */
  deleteAll: async () => {
    await api.delete(API_ENDPOINTS.ICE_BREAKERS);
  },
};

export default iceBreakerService;
