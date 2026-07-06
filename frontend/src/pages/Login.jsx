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
import GoogleButton  from '../components/form/GoogleButton.jsx';

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
      if (err?.code !== 'auth/popup-closed-by-user') {
        toast.error('Google sign-in failed. Please try again.');
      }
    } finally {
      setGoogleLoading(false);
    }
  };

  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-[0.14em] text-brand-600 dark:text-brand-400">
        Sign in
      </p>
      <h2 className="mt-2 font-display text-[34px] leading-tight text-ink-900 dark:text-ink-100">
        Welcome back
      </h2>
      <p className="mt-2 text-sm text-ink-500 dark:text-ink-400">
        Sign in to your CreatorEngine account.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-4" noValidate>
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
            className="text-sm font-medium text-brand-700 transition-colors hover:text-brand-800 dark:text-brand-400 dark:hover:text-brand-300"
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

      <div className="relative my-7">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-ink-200 dark:border-ink-800" />
        </div>
        <div className="relative flex justify-center text-xs uppercase tracking-wider">
          <span className="bg-white px-3 text-ink-400 dark:bg-ink-950">or continue with</span>
        </div>
      </div>

      <GoogleButton onClick={onGoogleSignIn} loading={googleLoading} />

      <p className="mt-8 text-center text-sm text-ink-500 dark:text-ink-400">
        Don&apos;t have an account?{' '}
        <Link
          to={ROUTES.REGISTER}
          className="font-medium text-ink-900 underline-offset-4 hover:underline dark:text-ink-100"
        >
          Create one
        </Link>
      </p>
    </div>
  );
};

export default Login;