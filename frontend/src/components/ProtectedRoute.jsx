import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore.js';
import { ROUTES } from '../utils/constants.js';
import PageLoader from './PageLoader.jsx';

/**
 * Wraps a tree of routes that require authentication.
 *
 *  - While the auth store is still hydrating from storage, show a loader
 *    (prevents a flash of /login on the first render).
 *  - If not authenticated, redirect to /login while remembering where
 *    the user was trying to go so we can send them back after login.
 */
const ProtectedRoute = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isHydrated = useAuthStore((s) => s.isHydrated);
  const location = useLocation();

  if (!isHydrated) return <PageLoader />;

  if (!isAuthenticated) {
    return (
      <Navigate
        to={ROUTES.LOGIN}
        state={{ from: location.pathname }}
        replace
      />
    );
  }

  return <Outlet />;
};

export default ProtectedRoute;
