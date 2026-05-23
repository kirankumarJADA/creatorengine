import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

/**
 * Thin REST client for /api/automations/*.
 *
 * Every method returns the unwrapped `data` payload (the axios
 * response interceptor strips the {success, data, message} envelope).
 */
const automationService = {
  list: async () => {
    const { data } = await api.get(API_ENDPOINTS.AUTOMATIONS);
    return data; // → AutomationResponse[]
  },

  get: async (id) => {
    const { data } = await api.get(API_ENDPOINTS.AUTOMATION_BY_ID(id));
    return data;
  },

  create: async (payload) => {
    const { data } = await api.post(API_ENDPOINTS.AUTOMATIONS, payload);
    return data;
  },

  update: async (id, payload) => {
    const { data } = await api.put(API_ENDPOINTS.AUTOMATION_BY_ID(id), payload);
    return data;
  },

  remove: async (id) => {
    await api.delete(API_ENDPOINTS.AUTOMATION_BY_ID(id));
  },

  toggle: async (id, enabled) => {
    const { data } = await api.patch(API_ENDPOINTS.AUTOMATION_TOGGLE(id), {
      enabled,
    });
    return data;
  },
};

export default automationService;
