import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Mail, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';

import FormField from '../components/form/FormField.jsx';
import PasswordField from '../components/form/PasswordField.jsx';
import Button from '../components/form/Button.jsx';
import Checkbox from '../components/form/Checkbox.jsx';

import { useAuthStore } from '../store/authStore.js';
import { ROUTES } from '../utils/constants.js';
import { EMAIL_RULES, LOGIN_PASSWORD_RULES } from '../utils/validators.js';

const Login = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((s) => s.login);
  const isLoading = useAuthStore((s) => s.isLoading);

  // After login, send the user back where they came from (if they were
  // redirected here by ProtectedRoute) — falls back to /dashboard.
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
      // The axios interceptor already showed the error toast.
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
