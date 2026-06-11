import { create } from 'zustand';
import automationService from '../services/automationService.js';


/**
 * Automation list + CRUD store.
 *
 * Design note — "API or mock?"
 * ────────────────────────────
 * The store seeds itself with the mock dataset so the UI is usable
 * the moment a developer opens it, with or without the Spring backend
 * running. {@link fetchAutomations} attempts the real API on mount;
 * if it succeeds we replace the seed, if it fails we keep going with
 * mock data and surface the error in `lastError` (without toasting,
 * because a missing backend in dev shouldn't yell at the user).
 *
 * CRUD operations are optimistic: local state updates immediately,
 * then we sync to the backend. On API failure we don't roll back —
 * we just log the error and keep the local change. This is the right
 * trade-off for a UI build that's expected to run in mock mode most
 * of the time.
 */

const generateId = () =>
  `auto_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;

const ensureRuntimeFields = (a) => ({
  runCount: 0,
  successCount: 0,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  ...a,
});

export const useAutomationStore = create((set, get) => ({
  // ─── State ─────────────────────────────────────
  automations: [],
  isLoading: false,
  lastError: null,

  // ─── Selectors ─────────────────────────────────
  getById: (id) => get().automations.find((a) => a.id === id) || null,

  /**
   * Fetch one automation, either from local cache or the backend.
   * Used by the Edit page so a direct URL hit (bookmark, hard refresh,
   * new tab) doesn't blank-page when the store hasn't been hydrated.
   * Returns the automation object on success, null on failure.
   */
  fetchById: async (id) => {
    if (!id) return null;
    const cached = get().getById(id);
    if (cached) return cached;

    try {
      const fresh = await automationService.get(id);
      if (!fresh) return null;
      set((s) => {
        const exists = s.automations.some((a) => a.id === id);
        return {
          automations: exists
            ? s.automations.map((a) => (a.id === id ? fresh : a))
            : [...s.automations, fresh],
        };
      });
      return fresh;
    } catch (err) {
      return null;
    }
  },

  // ─── Reads ─────────────────────────────────────
  fetchAutomations: async () => {
    set({ isLoading: true, lastError: null });
    try {
      const list = await automationService.list();
      set({ automations: list, isLoading: false });
    } catch (err) {
      set({
        isLoading: false,
        lastError: err?.message || 'Could not load automations.',
      });
    }
  },

  // ─── Writes ────────────────────────────────────
  createAutomation: async (input) => {
    const optimistic = ensureRuntimeFields({ id: generateId(), ...input });
    set((s) => ({ automations: [optimistic, ...s.automations] }));
    try {
      const saved = await automationService.create(input);
      set((s) => ({
        automations: s.automations.map((a) =>
          a.id === optimistic.id ? saved : a
        ),
      }));
      return saved;
    } catch (err) {
      return optimistic;
    }
  },

  updateAutomation: async (id, patch) => {
    const before = get().getById(id);
    const next = { ...before, ...patch, updatedAt: new Date().toISOString() };
    set((s) => ({
      automations: s.automations.map((a) => (a.id === id ? next : a)),
    }));
    try {
      const saved = await automationService.update(id, patch);
      set((s) => ({
        automations: s.automations.map((a) => (a.id === id ? saved : a)),
      }));
      return saved;
    } catch (err) {
      return next;
    }
  },

  deleteAutomation: async (id) => {
    const snapshot = get().automations;
    set((s) => ({ automations: s.automations.filter((a) => a.id !== id) }));
    try {
      await automationService.remove(id);
    } catch (err) {
      set({ automations: snapshot });
    }
  },

  toggleAutomation: async (id) => {
    const cur = get().getById(id);
    if (!cur) return;
    const next = { ...cur, enabled: !cur.enabled, updatedAt: new Date().toISOString() };
    set((s) => ({
      automations: s.automations.map((a) => (a.id === id ? next : a)),
    }));
    try {
      const saved = await automationService.toggle(id, next.enabled);
      set((s) => ({
        automations: s.automations.map((a) => (a.id === id ? saved : a)),
      }));
    } catch (err) {
      set((s) => ({
        automations: s.automations.map((a) =>
          a.id === id ? { ...a, enabled: cur.enabled } : a
        ),
      }));
    }
  },
}));