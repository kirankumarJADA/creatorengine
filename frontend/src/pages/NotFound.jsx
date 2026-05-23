import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';

import Button from '../components/form/Button.jsx';
import { useAuthStore } from '../store/authStore.js';
import { ROUTES } from '../utils/constants.js';

/**
 * 404 fallback. Sends the user home — wherever "home" is given their
 * current auth state. Authenticated users land on the dashboard;
 * everyone else lands on the login page.
 */
const NotFound = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const homePath = isAuthenticated ? ROUTES.DASHBOARD : ROUTES.LOGIN;

  return (
    <div className="grid min-h-screen place-items-center bg-ink-50 px-6 text-center">
      <div className="max-w-md">
        <p className="font-mono text-sm font-medium text-brand-700">
          404
        </p>
        <h1 className="mt-2 font-display text-4xl font-semibold text-ink-900">
          Page not found
        </h1>
        <p className="mt-2 text-ink-500">
          The page you&apos;re looking for doesn&apos;t exist or has been
          moved.
        </p>
        <div className="mt-6">
          <Link to={homePath}>
            <Button leftIcon={ArrowLeft}>
              {isAuthenticated ? 'Back to dashboard' : 'Back to sign in'}
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default NotFound;
