import axios from 'axios';
import toast from 'react-hot-toast';
import { storage } from '../utils/storage.js';
import { STORAGE_KEYS, ROUTES } from '../utils/constants.js';

/**
 * Centralised axios instance.
 *
 * Responsibilities:
 *   - Attach the JWT access token to every outgoing request.
 *   - Unwrap the backend's { success, data, message } envelope so
 *     callers see `response.data` as the actual payload.
 *   - Surface readable error messages via toast when the response is 4xx/5xx.
 *   - On 401, clear local auth state and redirect to /login (a manual
 *     refresh-token retry isn't part of this auth-only foundation, but
 *     the hook is there if needed).
 */
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// ─── Request: attach Authorization header ──────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = storage.get(STORAGE_KEYS.ACCESS_TOKEN);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ─── Response: unwrap envelope + central error handling ────────────
api.interceptors.response.use(
  (response) => {
    const body = response.data;
    // Backend wraps payloads as { success, data, message }. Unwrap `data`
    // so callers don't have to know about the envelope. Keep the full
    // envelope under `response.raw` for endpoints that want it.
    if (body && typeof body === 'object' && 'data' in body) {
      return { ...response, data: body.data, raw: body };
    }
    return response;
  },
  (error) => {
    const status = error.response?.status;
    const apiData = error.response?.data;
    const message =
      apiData?.message ||
      (Array.isArray(apiData?.errors) ? apiData.errors[0] : null) ||
      error.message ||
      'Something went wrong.';

    // Network error / timeout
    if (!error.response) {
      toast.error('Network error — please check your connection.');
      return Promise.reject(error);
    }

    // 401 — kill the session unless we explicitly silenced it
    if (status === 401 && !error.config?.silent) {
      storage.remove(STORAGE_KEYS.ACCESS_TOKEN);
      storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
      storage.remove(STORAGE_KEYS.USER);
      // Avoid bouncing if we're already on an auth page
      const onAuthPage = [
        ROUTES.LOGIN,
        ROUTES.REGISTER,
        ROUTES.FORGOT_PASSWORD,
      ].includes(window.location.pathname);
      if (!onAuthPage) {
        toast.error('Your session has expired. Please log in again.');
        window.location.href = ROUTES.LOGIN;
      }
      return Promise.reject(error);
    }

    // Other 4xx/5xx — show the toast unless the caller silenced it.
    // Validation errors (errors array) tend to look better as a single line.
    if (!error.config?.silent) {
      toast.error(message);
    }

    return Promise.reject(error);
  }
);

export default api;
