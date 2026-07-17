import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

const aiFaqService = {
  /** Fetch current AI FAQ config + plan eligibility */
  get: async () => {
    const { data } = await api.get(API_ENDPOINTS.AI_FAQ);
    return data;
  },

  /** Save AI FAQ config (enabled, knowledgeBase, qaPairs). Pro/Agency only. */
  save: async (config) => {
    const { data } = await api.put(API_ENDPOINTS.AI_FAQ, config);
    return data;
  },
};

export default aiFaqService;
