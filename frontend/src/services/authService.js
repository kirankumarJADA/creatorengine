import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

/**
 * Auth API surface. Each method returns the unwrapped backend `data`.
 */
const authService = {
  register: async ({ name, email, password }) => {
    const { data } = await api.post(API_ENDPOINTS.REGISTER, { name, email, password });
    return data;
  },

  login: async ({ email, password }) => {
    const { data } = await api.post(API_ENDPOINTS.LOGIN, { email, password });
    return data;
  },

  logout: async () => {
    try {
      await api.post(API_ENDPOINTS.LOGOUT, null, { silent: true });
    } catch {
      /* best-effort no-op for stateless JWT */
    }
  },

  me: async () => {
    const { data } = await api.get(API_ENDPOINTS.ME);
    return data;
  },

  updateProfile: async ({ name }) => {
    const { data } = await api.patch('/auth/profile', { name });
    return data; // updated user
  },

  changePassword: async ({ currentPassword, newPassword }) => {
    await api.post('/auth/change-password', { currentPassword, newPassword });
  },

  forgotPassword: async ({ email }) => {
    const { raw } = await api.post(API_ENDPOINTS.FORGOT_PASSWORD, { email });
    return raw;
  },
};

export default authService;