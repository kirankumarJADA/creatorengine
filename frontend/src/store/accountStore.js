import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import instagramService from '../services/instagramService.js';

/**
 * Tracks which Instagram account is currently active in the workspace.
 * Persisted to localStorage so the selection survives page refreshes.
 *
 * All pages that show per-account data (Automations, Contacts, Logs, etc.)
 * should read `activeAccount` from this store and pass it as a filter.
 */
export const useAccountStore = create(
  persist(
    (set, get) => ({
      accounts: [],          // all connected IG accounts
      activeAccount: null,   // the currently selected one
      isLoading: false,

      /**
       * Fetch all connected accounts and set the active one.
       * Called on app load and after connect/disconnect.
       */
      fetchAccounts: async () => {
        set({ isLoading: true });
        try {
          const accounts = await instagramService.getAccounts();
          const current = get().activeAccount;

          // Keep the previously selected account if it's still connected,
          // otherwise default to the first account.
          const stillConnected = current
            ? accounts.find((a) => a.instagramUserId === current.instagramUserId)
            : null;

          set({
            accounts,
            activeAccount: stillConnected || accounts[0] || null,
            isLoading: false,
          });
        } catch {
          set({ isLoading: false });
        }
      },

      setActiveAccount: (account) => set({ activeAccount: account }),

      /** Called after connecting a new account — re-fetches the list. */
      onAccountConnected: async () => {
        await get().fetchAccounts();
      },

      /** Called after disconnecting an account — re-fetches the list. */
      onAccountDisconnected: async () => {
        await get().fetchAccounts();
      },
    }),
    {
      name: 'ce.active_account',
      // Only persist the active account ID — re-fetch the full list on load
      partialize: (state) => ({
        activeAccount: state.activeAccount
          ? { instagramUserId: state.activeAccount.instagramUserId }
          : null,
      }),
    }
  )
);