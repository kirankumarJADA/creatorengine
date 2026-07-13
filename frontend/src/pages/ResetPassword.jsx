import { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import toast from 'react-hot-toast';

import PasswordField from '../components/form/PasswordField.jsx';
import Button from '../components/form/Button.jsx';
import { ROUTES } from '../utils/constants.js';
import authService from '../services/authService.js';

const ResetPassword = () => {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const oobCode = params.get('oobCode');

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (password.length < 8) {
      toast.error('Password must be at least 8 characters.');
      return;
    }
    if (password !== confirm) {
      toast.error('Passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      await authService.confirmReset({ oobCode, newPassword: password });
      setDone(true);
      toast.success('Password reset successfully.');
    } catch {
      toast.error('This link is invalid or has expired.');
    } finally {
      setLoading(false);
    }
  };

  if (!oobCode) {
    return (
      <div>
        <h2 className="font-display text-3xl text-ink-900 dark:text-ink-100">
          Invalid link
        </h2>
        <p className="mt-2 text-sm text-ink-500 dark:text-ink-400">
          This password reset link is missing or malformed.
        </p>
        <Link
          to={ROUTES.LOGIN}
          className="mt-6 inline-block font-medium text-brand-600 hover:underline dark:text-brand-400"
        >
          Back to login
        </Link>
      </div>
    );
  }

  if (done) {
    return (
      <div>
        <h2 className="font-display text-3xl text-ink-900 dark:text-ink-100">
          Password updated
        </h2>
        <p className="mt-2 text-sm text-ink-500 dark:text-ink-400">
          Your password has been reset. You can now sign in.
        </p>
        <Button
          size="lg"
          className="mt-6 w-full"
          onClick={() => navigate(ROUTES.LOGIN)}
        >
          Go to login
        </Button>
      </div>
    );
  }

  return (
    <div>
      <h2 className="font-display text-3xl text-ink-900 dark:text-ink-100">
        Set a new password
      </h2>
      <p className="mt-2 text-sm text-ink-500 dark:text-ink-400">
        Choose a new password for your account.
      </p>

      <form onSubmit={onSubmit} className="mt-8 space-y-4">
        <PasswordField
          label="New password"
          placeholder="At least 8 characters"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <PasswordField
          label="Confirm new password"
          placeholder="Re-enter your password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
        />
        <Button type="submit" size="lg" isLoading={loading} className="w-full">
          Reset password
        </Button>
      </form>
    </div>
  );
};

export default ResetPassword;