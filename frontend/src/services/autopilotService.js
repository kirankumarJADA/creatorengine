import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

const autopilotService = {
  /** Fetch current AI Autopilot config + plan eligibility */
  get: async () => {
    const { data } = await api.get(API_ENDPOINTS.AI_AUTOPILOT);
    return data;
  },

  /** Save AI Autopilot config. Pro/Agency only. */
  save: async (config) => {
    const { data } = await api.put(API_ENDPOINTS.AI_AUTOPILOT, config);
    return data;
  },

  /** Usage stats for the active account. */
  stats: async () => {
    const { data } = await api.get(`${API_ENDPOINTS.AI_AUTOPILOT}/stats`);
    return data;
  },
};

export default autopilotService;
