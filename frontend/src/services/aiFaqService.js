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

  /** Test the draft (possibly unsaved) knowledge base/Q&A against a sample question. */
  test: async ({ knowledgeBase, qaPairs, message }) => {
    const { data } = await api.post(`${API_ENDPOINTS.AI_FAQ}/test`, { knowledgeBase, qaPairs, message });
    return data;
  },
};

export default aiFaqService;
