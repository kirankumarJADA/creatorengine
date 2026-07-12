import axios from 'axios';
import { useAuthStore } from '../store/authStore.js';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: false,
});

// ─── Request interceptor ──────────────────────────────────────────
// Attaches the Firebase JWT and the active Instagram account ID to
// every outgoing request. The account ID is read directly from
// localStorage (where accountStore persists it) to avoid a circular
// import between api.js and accountStore.js.
api.interceptors.request.use(async (config) => {
  // Attach Firebase auth token
  const { accessToken } = useAuthStore.getState();

  if (accessToken) {
    config.headers['Authorization'] = `Bearer ${accessToken}`;
  }

  // Attach active Instagram account ID for per-account scoping.
  // Read from persisted accountStore state in localStorage.
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

// ─── Response interceptor ─────────────────────────────────────────
// Unwraps the {success, data, message} envelope returned by the
// backend so callers get the payload directly in response.data.
api.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body === 'object' && 'data' in body) {
      response.data = body.data;
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  }
);

export default api;