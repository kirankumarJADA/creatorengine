/**
 * Thin wrapper around localStorage with safe JSON (de)serialisation
 * and SSR-friendly guards. Centralising it means we can later swap
 * the backend (sessionStorage, encrypted store) without touching
 * call sites.
 */

const isBrowser = typeof window !== 'undefined' && !!window.localStorage;

const safeParse = (raw) => {
  if (raw === null) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
};

export const storage = {
  get(key, fallback = null) {
    if (!isBrowser) return fallback;
    const raw = window.localStorage.getItem(key);
    return raw === null ? fallback : safeParse(raw);
  },

  set(key, value) {
    if (!isBrowser) return;
    const serialized = typeof value === 'string' ? value : JSON.stringify(value);
    window.localStorage.setItem(key, serialized);
  },

  remove(key) {
    if (!isBrowser) return;
    window.localStorage.removeItem(key);
  },

  clear() {
    if (!isBrowser) return;
    window.localStorage.clear();
  },
};
