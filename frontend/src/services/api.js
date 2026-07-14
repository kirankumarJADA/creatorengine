import axios from 'axios';
import { useAuthStore } from '../store/authStore.js';
import { storage } from '../utils/storage.js';
import { STORAGE_KEYS } from '../utils/constants.js';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: false,
});

// ─── Token refresh state ────────────────────────────────────────────────────
// Prevents multiple simultaneous 401s from each triggering their own refresh.
let isRefreshing = false;
let refreshQueue = []; // { resolve, reject }[]

const processQueue = (error, token = null) => {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token);
  });
  refreshQueue = [];
};

// ─── Request interceptor ────────────────────────────────────────────────────
api.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    config.headers['Authorization'] = `Bearer ${accessToken}`;
  }

  // Attach active Instagram account ID for per-account scoping
  try {
    const stored = localStorage.getItem('ce.active_account');
    if (stored) {
      const parsed = JSON.parse(stored);
      const igId = parsed?.state?.activeAccount?.instagramUserId;
      if (igId) {
        config.headers['X-IG-Account-Id'] = igId;
      }
    }
  } catch {
    // Ignore — header just won't be set
  }

  return config;
});

// ─── Response interceptor ───────────────────────────────────────────────────
api.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body === 'object' && 'data' in body) {
      response.data = body.data;
    }
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;

    // Only handle 401s, and never retry the refresh endpoint itself
    if (status !== 401 || originalRequest._retry || originalRequest.url?.includes('/auth/refresh')) {
      return Promise.reject(error);
    }

    const { refreshToken } = useAuthStore.getState();

    // No refresh token — just log out immediately
    if (!refreshToken) {
      useAuthStore.getState().logout();
      return Promise.reject(error);
    }

    // Another refresh is already in flight — queue this request
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        refreshQueue.push({ resolve, reject });
      }).then((newToken) => {
        originalRequest._retry = true;
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return api(originalRequest);
      }).catch((err) => Promise.reject(err));
    }

    // Start a fresh refresh
    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const resp = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken });
      // The raw axios call doesn't go through our unwrap interceptor
      const body = resp.data;
      const authData = (body && 'data' in body) ? body.data : body;
      const { accessToken: newAccess, refreshToken: newRefresh, user } = authData;

      // Persist new tokens
      if (newAccess) storage.set(STORAGE_KEYS.ACCESS_TOKEN, newAccess);
      if (newRefresh) storage.set(STORAGE_KEYS.REFRESH_TOKEN, newRefresh);
      if (user) storage.set(STORAGE_KEYS.USER, user);
      useAuthStore.getState()._persistSession({
        accessToken: newAccess,
        refreshToken: newRefresh,
        user,
      });

      processQueue(null, newAccess);
      originalRequest.headers['Authorization'] = `Bearer ${newAccess}`;
      return api(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      useAuthStore.getState().logout();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;