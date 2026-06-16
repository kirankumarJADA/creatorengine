import axios from 'axios';
import toast from 'react-hot-toast';
import { storage } from '../utils/storage.js';
import { STORAGE_KEYS, ROUTES } from '../utils/constants.js';

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// ─── Request: attach Authorization header ──────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = storage.get(STORAGE_KEYS.ACCESS_TOKEN);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // Stash the ORIGINAL (un-serialized) body so a retry after token refresh
    // doesn't double-encode it.
    if (config.data !== undefined && config.__rawData === undefined) {
      config.__rawData = config.data;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ─── Token refresh plumbing ────────────────────────────────────────────────
let isRefreshing = false;
let pendingQueue = [];

const flushQueue = (error, token = null) => {
  pendingQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token);
  });
  pendingQueue = [];
};

const clearSession = () => {
  storage.remove(STORAGE_KEYS.ACCESS_TOKEN);
  storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
  storage.remove(STORAGE_KEYS.USER);
};

const redirectToLogin = () => {
  const onAuthPage = [
    ROUTES.LOGIN,
    ROUTES.REGISTER,
    ROUTES.FORGOT_PASSWORD,
  ].includes(window.location.pathname);
  if (!onAuthPage) {
    window.location.href = ROUTES.LOGIN;
  }
};

const resendOriginal = (originalRequest, token) => {
  if (originalRequest.__rawData !== undefined) {
    originalRequest.data = originalRequest.__rawData;
  }
  originalRequest.headers = originalRequest.headers || {};
  originalRequest.headers.Authorization = `Bearer ${token}`;
  return api(originalRequest);
};

// ─── Response: unwrap envelope + refresh-on-401 + error handling ────────────
api.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body === 'object' && 'data' in body) {
      return { ...response, data: body.data, raw: body };
    }
    return response;
  },
  async (error) => {
    const originalRequest = error.config || {};
    const status = error.response?.status;

    // Network error / timeout
    if (!error.response) {
      if (!originalRequest.silent) {
        toast.error('Network error — please check your connection.');
      }
      return Promise.reject(error);
    }

    if (status === 401) {
      const url = originalRequest.url || '';

      // A 401 from these endpoints means BAD CREDENTIALS — not an expired
      // session. Never refresh, clear, or redirect. Just show the real reason
      // (e.g. "Incorrect email or password").
      const isCredentialEndpoint =
        url.includes('/auth/login') ||
        url.includes('/auth/register') ||
        url.includes('/auth/refresh') ||
        url.includes('/auth/forgot-password');

      if (isCredentialEndpoint) {
        const apiData = error.response?.data;
        const message =
          apiData?.message ||
          (Array.isArray(apiData?.errors) ? apiData.errors[0] : null) ||
          'Incorrect email or password.';
        if (!originalRequest.silent) {
          toast.error(message);
        }
        return Promise.reject(error);
      }

      // Otherwise: a protected call whose access token likely expired → refresh.
      const refreshToken = storage.get(STORAGE_KEYS.REFRESH_TOKEN);

      if (!originalRequest._retry && refreshToken) {
        originalRequest._retry = true;

        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            pendingQueue.push({ resolve, reject });
          }).then((newToken) => resendOriginal(originalRequest, newToken));
        }

        isRefreshing = true;
        try {
          const resp = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });
          const data = resp.data?.data ?? resp.data;
          const newAccess = data.accessToken;
          const newRefresh = data.refreshToken;

          storage.set(STORAGE_KEYS.ACCESS_TOKEN, newAccess);
          if (newRefresh) storage.set(STORAGE_KEYS.REFRESH_TOKEN, newRefresh);
          if (data.user) storage.set(STORAGE_KEYS.USER, data.user);

          flushQueue(null, newAccess);
          return resendOriginal(originalRequest, newAccess);
        } catch (refreshError) {
          flushQueue(refreshError, null);
          clearSession();
          if (!originalRequest.silent) {
            toast.error('Your session has expired. Please log in again.');
            redirectToLogin();
          }
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      }

      // No refresh possible → session is genuinely over.
      clearSession();
      if (!originalRequest.silent) {
        toast.error('Your session has expired. Please log in again.');
        redirectToLogin();
      }
      return Promise.reject(error);
    }

    // ─── Other 4xx/5xx ───
    const apiData = error.response?.data;
    const message =
      apiData?.message ||
      (Array.isArray(apiData?.errors) ? apiData.errors[0] : null) ||
      error.message ||
      'Something went wrong.';

    if (!originalRequest.silent) {
      toast.error(message);
    }

    return Promise.reject(error);
  }
);

export default api;