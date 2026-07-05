import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Mail, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';
import { useState } from 'react';
import { GoogleAuthProvider, signInWithPopup } from 'firebase/auth';

import FormField     from '../components/form/FormField.jsx';
import PasswordField from '../components/form/PasswordField.jsx';
import Button        from '../components/form/Button.jsx';
import Checkbox      from '../components/form/Checkbox.jsx';

import { useAuthStore }  from '../store/authStore.js';
import { ROUTES }        from '../utils/constants.js';
import { EMAIL_RULES, LOGIN_PASSWORD_RULES } from '../utils/validators.js';
import authService from '../services/authService.js';
import { auth } from '../firebase.js';

const Login = () => {
  const navigate        = useNavigate();
  const location        = useLocation();
  const login           = useAuthStore((s) => s.login);
  const persistSession  = useAuthStore((s) => s._persistSession);
  const isLoading       = useAuthStore((s) => s.isLoading);
  const [googleLoading, setGoogleLoading] = useState(false);

  const from = location.state?.from || ROUTES.DASHBOARD;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: { email: '', password: '', remember: true },
    mode: 'onTouched',
  });

  const onSubmit = async (values) => {
    try {
      await login({ email: values.email, password: values.password });
      toast.success('Welcome back!');
      navigate(from, { replace: true });
    } catch {
      // interceptor already toasted
    }
  };

  const onGoogleSignIn = async () => {
    setGoogleLoading(true);
    try {
      const provider = new GoogleAuthProvider();
      const result   = await signInWithPopup(auth, provider);
      const idToken  = await result.user.getIdToken();

      const data = await authService.googleSignIn({ idToken });
      persistSession(data);
      toast.success('Welcome!');
      navigate(from, { replace: true });
    } catch (err) {
      console.error('GOOGLE SIGNIN ERROR:', err?.code, err?.message, err);
      if (err?.code !== 'auth/popup-closed-by-user') {
        toast.error('Google sign-in failed. Please try again.');
      }
    } finally {
      setGoogleLoading(false);
    }
  };

  return (
    <div>
      <h2 className="font-display text-3xl text-ink-900">Welcome back</h2>
      <p className="mt-2 text-sm text-ink-500">
        Sign in to your CreatorEngine account.
      </p>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="mt-8 space-y-4"
        noValidate
      >
        <FormField
          label="Email"
          type="email"
          placeholder="you@company.com"
          autoComplete="email"
          leftIcon={Mail}
          error={errors.email?.message}
          {...register('email', EMAIL_RULES)}
        />

        <PasswordField
          label="Password"
          placeholder="••••••••"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password', LOGIN_PASSWORD_RULES)}
        />

        <div className="flex items-center justify-between">
          <Checkbox label="Remember me" {...register('remember')} />
          <Link
            to={ROUTES.FORGOT_PASSWORD}
            className="text-sm font-medium text-brand-700 hover:text-brand-800"
          >
            Forgot password?
          </Link>
        </div>

        <Button
          type="submit"
          size="lg"
          isLoading={isLoading}
          rightIcon={ArrowRight}
          className="w-full"
        >
          Sign in
        </Button>
      </form>

      <div className="relative my-6">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-ink-200" />
        </div>
        <div className="relative flex justify-center text-sm">
          <span className="bg-white px-3 text-ink-400">or</span>
        </div>
      </div>

      <button
        type="button"
        onClick={onGoogleSignIn}
        disabled={googleLoading}
        className="w-full flex items-center justify-center gap-3 rounded-lg border
                   border-ink-200 bg-white px-4 py-3 text-sm font-medium
                   text-ink-700 shadow-sm hover:bg-ink-50 transition
                   disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {googleLoading ? (
          <span className="h-5 w-5 animate-spin rounded-full border-2
                           border-ink-300 border-t-ink-700" />
        ) : (
          <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
        )}
        Continue with Google
      </button>

      <p className="mt-8 text-center text-sm text-ink-500">
        Don&apos;t have an account?{' '}
        <Link
          to={ROUTES.REGISTER}
          className="font-medium text-ink-900 hover:underline"
        >
          Create one
        </Link>
      </p>
    </div>
  );
};

export default Login;