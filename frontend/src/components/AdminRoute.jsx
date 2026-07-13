import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/authStore.js';
import { ROUTES } from '../utils/constants.js';
import PageLoader from './PageLoader.jsx';

/**
 * Wraps admin-only routes. Mirrors ProtectedRoute's hydration guard,
 * then additionally checks for the ADMIN role. This is a UX nicety —
 * the real security boundary is @PreAuthorize on the backend, which
 * rejects any non-admin token regardless of what the frontend renders.
 */
const AdminRoute = () => {
  const isHydrated = useAuthStore((s) => s.isHydrated);
  const user = useAuthStore((s) => s.user);

  if (!isHydrated) return <PageLoader />;

  const isAdmin = Array.isArray(user?.roles) && user.roles.includes('ADMIN');

  if (!isAdmin) {
    return <Navigate to={ROUTES.DASHBOARD} replace />;
  }

  return <Outlet />;
};

export default AdminRoute;