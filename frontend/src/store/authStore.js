import { create } from 'zustand';
import authService from '../services/authService.js';
import { storage } from '../utils/storage.js';
import { STORAGE_KEYS } from '../utils/constants.js';

/**
 * Auth store — owns the user session.
 *
 * Persistence is hand-rolled (not zustand/persist) because we want
 * fine-grained control over which keys hit localStorage and we share
 * those keys with the axios interceptor in services/api.js. The
 * `isHydrated` flag prevents protected routes from redirecting to
 * /login during the very first render before the store has read
 * from storage.
 */
export const useAuthStore = create((set, get) => ({
  // ─── State ─────────────────────────────────────────
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isHydrated: false, // becomes true after bootstrap() runs
  isLoading: false,

  // ─── Hydration ─────────────────────────────────────
  bootstrap: () => {
    const accessToken = storage.get(STORAGE_KEYS.ACCESS_TOKEN);
    const refreshToken = storage.get(STORAGE_KEYS.REFRESH_TOKEN);
    const user = storage.get(STORAGE_KEYS.USER);

    set({
      accessToken,
      refreshToken,
      user,
      isAuthenticated: !!accessToken,
      isHydrated: true,
    });
  },

  // ─── Actions ───────────────────────────────────────
  login: async (credentials) => {
    set({ isLoading: true });
    try {
      const data = await authService.login(credentials);
      get()._persistSession(data);
      return data.user;
    } finally {
      set({ isLoading: false });
    }
  },

  register: async (payload) => {
    set({ isLoading: true });
    try {
      const data = await authService.register(payload);
      get()._persistSession(data);
      return data.user;
    } finally {
      set({ isLoading: false });
    }
  },

  logout: async () => {
    await authService.logout();
    storage.remove(STORAGE_KEYS.ACCESS_TOKEN);
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
    storage.remove(STORAGE_KEYS.USER);
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
    });
  },

  refreshUser: async () => {
    const user = await authService.me();
    storage.set(STORAGE_KEYS.USER, user);
    set({ user });
    return user;
  },

  // ─── Internal helpers ──────────────────────────────
  _persistSession: ({ user, accessToken, refreshToken }) => {
    if (accessToken) storage.set(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    if (refreshToken) storage.set(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
    if (user) storage.set(STORAGE_KEYS.USER, user);
    set({
      user,
      accessToken,
      refreshToken,
      isAuthenticated: !!accessToken,
    });
  },
}));
