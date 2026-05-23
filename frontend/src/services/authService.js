import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

/**
 * Auth API surface.
 *
 * Each method returns the unwrapped backend `data` (the api
 * interceptor strips the envelope automatically).
 */
const authService = {
  register: async ({ name, email, password }) => {
    const { data } = await api.post(API_ENDPOINTS.REGISTER, {
      name,
      email,
      password,
    });
    return data; // { user, accessToken, refreshToken, tokenType, expiresIn }
  },

  login: async ({ email, password }) => {
    const { data } = await api.post(API_ENDPOINTS.LOGIN, { email, password });
    return data;
  },

  logout: async () => {
    try {
      await api.post(API_ENDPOINTS.LOGOUT, null, { silent: true });
    } catch {
      // Server-side logout is a best-effort no-op for stateless JWT —
      // swallow errors and let the caller clear local state regardless.
    }
  },

  me: async () => {
    const { data } = await api.get(API_ENDPOINTS.ME);
    return data;
  },

  forgotPassword: async ({ email }) => {
    const { raw } = await api.post(API_ENDPOINTS.FORGOT_PASSWORD, { email });
    return raw; // { success, message }
  },
};

export default authService;
