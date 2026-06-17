import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Mail, User, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';

import FormField from '../components/form/FormField.jsx';
import PasswordField from '../components/form/PasswordField.jsx';
import Button from '../components/form/Button.jsx';

import { useAuthStore } from '../store/authStore.js';
import { ROUTES } from '../utils/constants.js';
import {
  EMAIL_RULES,
  PASSWORD_RULES,
  NAME_RULES,
} from '../utils/validators.js';

// Only consumer Gmail / Outlook addresses are allowed at signup.
const ALLOWED_EMAIL_DOMAINS = [
  'gmail.com',
  'googlemail.com',
  'outlook.com',
  'hotmail.com',
  'live.com',
];

const isAllowedEmailDomain = (email) => {
  const domain = String(email || '').split('@')[1]?.toLowerCase().trim();
  return !!domain && ALLOWED_EMAIL_DOMAINS.includes(domain);
};

const Register = () => {
  const navigate = useNavigate();
  const registerUser = useAuthStore((s) => s.register);
  const isLoading = useAuthStore((s) => s.isLoading);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    defaultValues: { name: '', email: '', password: '', confirm: '' },
    mode: 'onTouched',
  });

  // We don't send the `confirm` field — it's only for client-side check.
  const password = watch('password');

  const onSubmit = async ({ confirm: _confirm, ...payload }) => {
    try {
      await registerUser(payload);
      toast.success('Account created — welcome to CreatorEngine!');
      navigate(ROUTES.DASHBOARD, { replace: true });
    } catch {
      /* interceptor toasted */
    }
  };

  return (
    <div>
      <h2 className="font-display text-3xl text-ink-900">Create your account</h2>
      <p className="mt-2 text-sm text-ink-500">
        Start building with CreatorEngine — no credit card required.
      </p>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="mt-8 space-y-4"
        noValidate
      >
        <FormField
          label="Full name"
          placeholder="Jane Doe"
          autoComplete="name"
          leftIcon={User}
          error={errors.name?.message}
          {...register('name', NAME_RULES)}
        />

        <FormField
          label="Email"
          type="email"
          placeholder="you@gmail.com"
          autoComplete="email"
          leftIcon={Mail}
          hint="Use a Gmail or Outlook email address."
          error={errors.email?.message}
          {...register('email', {
            ...EMAIL_RULES,
            validate: {
              ...(typeof EMAIL_RULES.validate === 'function'
                ? { emailFormat: EMAIL_RULES.validate }
                : EMAIL_RULES.validate),
              allowedDomain: (v) =>
                isAllowedEmailDomain(v) ||
                'Please use a Gmail or Outlook email address.',
            },
          })}
        />

        <PasswordField
          label="Password"
          placeholder="At least 8 characters"
          autoComplete="new-password"
          hint="Must contain at least one letter and one number."
          error={errors.password?.message}
          {...register('password', PASSWORD_RULES)}
        />

        <PasswordField
          label="Confirm password"
          placeholder="Re-enter your password"
          autoComplete="new-password"
          error={errors.confirm?.message}
          {...register('confirm', {
            required: 'Please confirm your password',
            validate: (v) => v === password || 'Passwords do not match',
          })}
        />

        <Button
          type="submit"
          size="lg"
          isLoading={isLoading}
          rightIcon={ArrowRight}
          className="w-full"
        >
          Create account
        </Button>

        <p className="text-center text-xs text-ink-500">
          By creating an account, you agree to our{' '}
          <a href="#" className="underline">
            Terms
          </a>{' '}
          and{' '}
          <a href="#" className="underline">
            Privacy Policy
          </a>
          .
        </p>
      </form>

      <p className="mt-8 text-center text-sm text-ink-500">
        Already have an account?{' '}
        <Link
          to={ROUTES.LOGIN}
          className="font-medium text-ink-900 hover:underline"
        >
          Sign in
        </Link>
      </p>
    </div>
  );
};

export default Register;