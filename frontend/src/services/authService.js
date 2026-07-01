import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

const authService = {
  sendOtp: async ({ email }) => {
    await api.post(API_ENDPOINTS.SEND_OTP, { email });
  },

  verifyOtp: async ({ email, otp, name, password }) => {
    const { data } = await api.post(API_ENDPOINTS.VERIFY_OTP,
        { email, otp, name, password });
    return data;
  },

  confirmReset: async ({ oobCode, newPassword }) => {
    await api.post(API_ENDPOINTS.CONFIRM_RESET, { oobCode, newPassword });
  },

  login: async ({ email, password }) => {
    const { data } = await api.post(API_ENDPOINTS.LOGIN, { email, password });
    return data;
  },

  logout: async () => {
    try {
      await api.post(API_ENDPOINTS.LOGOUT, null, { silent: true });
    } catch {
      /* best-effort */
    }
  },

  me: async () => {
    const { data } = await api.get(API_ENDPOINTS.ME);
    return data;
  },

  updateProfile: async ({ name }) => {
    const { data } = await api.patch('/auth/profile', { name });
    return data;
  },

  changePassword: async ({ currentPassword, newPassword }) => {
    await api.post('/auth/change-password', { currentPassword, newPassword });
  },

  forgotPassword: async ({ email }) => {
    await api.post(API_ENDPOINTS.FORGOT_PASSWORD, { email });
  },
};

export default authService;