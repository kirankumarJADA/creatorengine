import { create } from 'zustand';
import { storage } from '../utils/storage.js';
import { STORAGE_KEYS } from '../utils/constants.js';

/**
 * UI store — ephemeral global UI state.
 *
 * Three concerns live here:
 *  - Desktop sidebar: collapsed (icon-rail) vs expanded
 *  - Mobile drawer: open vs closed (overlay)
 *  - Theme: 'light' | 'dark', synced to <html class="dark"> and localStorage
 *
 * The theme initial value is *read* from the DOM rather than computed
 * fresh, because `index.html` already runs a synchronous bootstrapper
 * before React mounts (to avoid the flash-of-wrong-theme). This store
 * just mirrors what's already on <html>.
 */

const getSystemTheme = () =>
  typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light';

const readInitialTheme = () => {
  if (typeof document === 'undefined') return 'system';
  // Default to 'system' when nothing is stored
  return storage.get(STORAGE_KEYS.THEME) || 'system';
};

const readSidebarOpen = () => {
  const stored = storage.get(STORAGE_KEYS.SIDEBAR_OPEN);
  return stored === null || stored === undefined ? true : !!stored;
};

const applyTheme = (theme) => {
  if (typeof document === 'undefined') return;
  const effective = theme === 'system' ? getSystemTheme() : theme;
  document.documentElement.classList.toggle('dark', effective === 'dark');
};

export const useUiStore = create((set, get) => ({
  // ─── Sidebar (desktop) ─────────────────────────
  isSidebarOpen: readSidebarOpen(),
  toggleSidebar: () => {
    const next = !get().isSidebarOpen;
    storage.set(STORAGE_KEYS.SIDEBAR_OPEN, next);
    set({ isSidebarOpen: next });
  },
  setSidebarOpen: (isSidebarOpen) => {
    storage.set(STORAGE_KEYS.SIDEBAR_OPEN, isSidebarOpen);
    set({ isSidebarOpen });
  },

  // ─── Mobile drawer ─────────────────────────────
  isMobileNavOpen: false,
  toggleMobileNav: () =>
    set((s) => ({ isMobileNavOpen: !s.isMobileNavOpen })),
  setMobileNavOpen: (isMobileNavOpen) => set({ isMobileNavOpen }),

  // ─── Theme ─────────────────────────────────────
  theme: readInitialTheme(),
  // Cycles: light → dark → system → light …
  toggleTheme: () => {
    const cur = get().theme;
    const next = cur === 'light' ? 'dark' : cur === 'dark' ? 'system' : 'light';
    applyTheme(next);
    storage.set(STORAGE_KEYS.THEME, next);
    set({ theme: next });
  },
  setTheme: (theme) => {
    applyTheme(theme);
    storage.set(STORAGE_KEYS.THEME, theme);
    set({ theme });
  },
  // Call once on app mount — keeps 'system' mode in sync when the OS theme changes
  initThemeListener: () => {
    if (typeof window === 'undefined') return () => {};
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = () => {
      if (useUiStore.getState().theme === 'system') applyTheme('system');
    };
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  },
}));
