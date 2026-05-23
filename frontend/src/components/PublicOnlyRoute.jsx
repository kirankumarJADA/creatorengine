import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/authStore.js';
import { ROUTES } from '../utils/constants.js';
import PageLoader from './PageLoader.jsx';

/**
 * Inverse of ProtectedRoute: keeps logged-in users out of the
 * login/register/forgot-password pages.
 */
const PublicOnlyRoute = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isHydrated = useAuthStore((s) => s.isHydrated);

  if (!isHydrated) return <PageLoader />;

  if (isAuthenticated) return <Navigate to={ROUTES.DASHBOARD} replace />;

  return <Outlet />;
};

export default PublicOnlyRoute;
