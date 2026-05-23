import { useAuthStore } from '../store/authStore.js';

/**
 * Convenience selector hook for the auth store.
 *
 * <p>Components can still consume {@code useAuthStore} directly with a
 * narrower selector when they only need one field — that's the most
 * efficient option for renders. This hook is the all-purpose escape
 * hatch for places that need everything.</p>
 *
 * @example
 *   const { user, isAuthenticated, login, logout } = useAuth();
 */
export const useAuth = () => {
  return useAuthStore((s) => ({
    user: s.user,
    isAuthenticated: s.isAuthenticated,
    isLoading: s.isLoading,
    isHydrated: s.isHydrated,
    login: s.login,
    register: s.register,
    logout: s.logout,
    refreshUser: s.refreshUser,
  }));
};
