import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { KeyRound, Loader2, CheckCircle2, AlertTriangle } from 'lucide-react';

import Button from '../components/form/Button.jsx';
import PasswordField from '../components/form/PasswordField.jsx';
import { ROUTES } from '../utils/constants.js';

const IDENTITY_URL =
  'https://identitytoolkit.googleapis.com/v1/accounts:resetPassword';

const friendlyError = (code) => {
  if (!code) return 'Something went wrong. Please try again.';
  if (code.startsWith('WEAK_PASSWORD')) return 'Password is too weak. Use at least 8 characters.';
  if (code.includes('EXPIRED_OOB_CODE')) return 'This reset link has expired. Please request a new one.';
  if (code.includes('INVALID_OOB_CODE')) return 'This reset link is invalid. Please request a new one.';
  return 'Something went wrong. Please try again.';
};

const AuthAction = () => {
  const [params] = useSearchParams();
  const mode = params.get('mode');
  const oobCode = params.get('oobCode');
  const apiKey = params.get('apiKey');

  const [phase, setPhase] = useState('verifying'); // verifying | ready | invalid | success
  const [email, setEmail] = useState('');

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { next: '', confirm: '' } });
  const newPassword = watch('next');

  useEffect(() => {
    if (mode !== 'resetPassword' || !oobCode || !apiKey) {
      setPhase('invalid');
      return;
    }
    let active = true;
    (async () => {
      try {
        const res = await fetch(`${IDENTITY_URL}?key=${apiKey}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ oobCode }),
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data?.error?.message || 'INVALID');
        if (active) {
          setEmail(data.email || '');
          setPhase('ready');
        }
      } catch {
        if (active) setPhase('invalid');
      }
    })();
    return () => { active = false; };
  }, [mode, oobCode, apiKey]);

  const onSubmit = async ({ next }) => {
    try {
      const res = await fetch(`${IDENTITY_URL}?key=${apiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ oobCode, newPassword: next }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data?.error?.message || 'FAILED');
      setPhase('success');
    } catch (e) {
      toast.error(friendlyError(e.message));
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-ink-50 px-4 py-10 dark:bg-ink-950">
      <div className="w-full max-w-md rounded-2xl border border-ink-100 bg-white p-8 shadow-sm dark:border-ink-800 dark:bg-ink-900">
        <div className="mb-6 flex flex-col items-center text-center">
          <img src="/logo-mark.png" alt="CreatorEngine" className="h-12 w-12" />
          <h1 className="mt-3 text-xl font-semibold text-ink-900 dark:text-ink-100">
            CreatorEngine
          </h1>
        </div>

        {phase === 'verifying' && (
          <div className="flex flex-col items-center gap-3 py-8 text-ink-500 dark:text-ink-400">
            <Loader2 size={22} className="animate-spin" />
            <p className="text-sm">Checking your reset link…</p>
          </div>
        )}

        {phase === 'invalid' && (
          <div className="flex flex-col items-center gap-3 py-4 text-center">
            <span className="grid h-12 w-12 place-items-center rounded-full bg-amber-100 text-amber-600 dark:bg-amber-500/15 dark:text-amber-400">
              <AlertTriangle size={22} />
            </span>
            <h2 className="text-base font-semibold text-ink-900 dark:text-ink-100">
              Link invalid or expired
            </h2>
            <p className="text-sm text-ink-500 dark:text-ink-400">
              This password reset link is no longer valid. Please request a new one.
            </p>
            <Link to={ROUTES.FORGOT_PASSWORD} className="mt-2 w-full">
              <Button className="w-full">Request a new link</Button>
            </Link>
            <Link
              to={ROUTES.LOGIN}
              className="text-sm font-medium text-brand-600 hover:underline dark:text-brand-400"
            >
              Back to sign in
            </Link>
          </div>
        )}

        {phase === 'ready' && (
          <>
            <div className="mb-5 text-center">
              <h2 className="text-base font-semibold text-ink-900 dark:text-ink-100">
                Set a new password
              </h2>
              {email && (
                <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">for {email}</p>
              )}
            </div>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <PasswordField
                label="New password"
                leftIcon={KeyRound}
                error={errors.next?.message}
                {...register('next', {
                  required: 'New password is required',
                  minLength: { value: 8, message: 'At least 8 characters' },
                })}
              />
              <PasswordField
                label="Confirm new password"
                error={errors.confirm?.message}
                {...register('confirm', {
                  required: 'Please confirm your new password',
                  validate: (v) => v === newPassword || 'Passwords do not match',
                })}
              />
              <Button type="submit" isLoading={isSubmitting} className="w-full">
                Update password
              </Button>
            </form>
          </>
        )}

        {phase === 'success' && (
          <div className="flex flex-col items-center gap-3 py-4 text-center">
            <span className="grid h-12 w-12 place-items-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400">
              <CheckCircle2 size={22} />
            </span>
            <h2 className="text-base font-semibold text-ink-900 dark:text-ink-100">
              Password updated
            </h2>
            <p className="text-sm text-ink-500 dark:text-ink-400">
              You can now sign in with your new password.
            </p>
            <Link to={ROUTES.LOGIN} className="mt-2 w-full">
              <Button className="w-full">Go to sign in</Button>
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default AuthAction;