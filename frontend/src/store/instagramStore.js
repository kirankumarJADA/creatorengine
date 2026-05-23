import { create } from 'zustand';
import instagramService from '../services/instagramService.js';
import { CONNECTION_STATUS } from '../utils/constants.js';

/**
 * Owns the Instagram connection state for the current user.
 *
 * The store is intentionally tiny — there's at most one connected
 * account per user, and the only mutations are connect / disconnect.
 * Most of the cleverness lives on the backend.
 *
 * Lifecycle:
 *   fetchStatus()         — call on Settings mount and after the
 *                            OAuth callback returns success.
 *   startConnect()        — hits /connect, performs the browser
 *                            redirect to Meta. Doesn't return.
 *   disconnect()          — clears server state, then local state.
 */
export const useInstagramStore = create((set, get) => ({
  // ─── State ─────────────────────────────────────
  status:   CONNECTION_STATUS.NOT_CONNECTED,
  account:  null,   // { username, name, instagramUserId, ... } when connected
  isLoading: false, // status / disconnect in flight
  isConnecting: false, // /connect call in flight before browser redirect
  lastError: null,

  // ─── Selectors ─────────────────────────────────
  isConnected: () => get().status === CONNECTION_STATUS.CONNECTED,

  // ─── Actions ───────────────────────────────────
  fetchStatus: async () => {
    set({ isLoading: true, lastError: null });
    try {
      const data = await instagramService.getStatus();
      set({
        status:  data?.status || CONNECTION_STATUS.NOT_CONNECTED,
        account: data?.status && data.status !== CONNECTION_STATUS.NOT_CONNECTED
          ? data
          : null,
        isLoading: false,
      });
    } catch (err) {
      set({
        isLoading: false,
        lastError: err,
        // Don't reset to NOT_CONNECTED on transient errors — keep last-known.
      });
    }
  },

  /**
   * Asks the backend for the Meta authorization URL, then redirects
   * the browser to it. Resolves on failure only — on success the
   * page is unloaded, so anything after the redirect is moot.
   */
  startConnect: async () => {
    set({ isConnecting: true, lastError: null });
    try {
      const { authUrl } = await instagramService.startConnect();
      if (!authUrl) {
        throw new Error('Backend did not return an OAuth URL.');
      }
      // Hard navigation — we want Meta's domain to take over.
      window.location.href = authUrl;
    } catch (err) {
      set({ isConnecting: false, lastError: err });
      throw err;
    }
  },

  disconnect: async () => {
    set({ isLoading: true, lastError: null });
    try {
      await instagramService.disconnect();
      set({
        status: CONNECTION_STATUS.NOT_CONNECTED,
        account: null,
        isLoading: false,
      });
    } catch (err) {
      set({ isLoading: false, lastError: err });
      throw err;
    }
  },

  reset: () => set({
    status: CONNECTION_STATUS.NOT_CONNECTED,
    account: null,
    isLoading: false,
    isConnecting: false,
    lastError: null,
  }),
}));
