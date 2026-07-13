import { create } from 'zustand';
import automationService from '../services/automationService.js';

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
  automations: [],
  isLoading: false,
  lastError: null,

  getById: (id) => get().automations.find((a) => a.id === id) || null,

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

  // Called right before switching accounts so stale data never
  // flashes on screen while the new account's list is loading.
  clearAutomations: () => set({ automations: [], lastError: null }),
}));