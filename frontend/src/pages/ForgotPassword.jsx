import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Mail, ArrowLeft, CheckCircle2 } from 'lucide-react';
import toast from 'react-hot-toast';

import FormField from '../components/form/FormField.jsx';
import Button from '../components/form/Button.jsx';
import authService from '../services/authService.js';
import { ROUTES } from '../utils/constants.js';
import { EMAIL_RULES } from '../utils/validators.js';

const ForgotPassword = () => {
  const [submitted, setSubmitted] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: { email: '' },
    mode: 'onTouched',
  });

  const onSubmit = async ({ email }) => {
    try {
      await authService.forgotPassword({ email });
      // We don't acknowledge whether the email existed (to prevent enumeration),
      // so the success view always says "if an account exists…".
      setSubmittedEmail(email);
      setSubmitted(true);
      toast.success('Check your inbox.');
    } catch {
      /* interceptor toasted */
    }
  };

  if (submitted) {
    return (
      <div>
        <div className="grid h-12 w-12 place-items-center rounded-2xl bg-emerald-100 text-emerald-700">
          <CheckCircle2 size={22} />
        </div>
        <h2 className="mt-5 font-display text-3xl text-ink-900">
          Check your email
        </h2>
        <p className="mt-2 text-sm text-ink-500">
          If an account exists for{' '}
          <strong className="text-ink-800">{submittedEmail}</strong>, we&apos;ve
          sent a link to reset your password. The link will expire in 1 hour.
        </p>
        <div className="mt-8 space-y-3">
          <Link to={ROUTES.LOGIN}>
            <Button leftIcon={ArrowLeft} variant="secondary" className="w-full">
              Back to sign in
            </Button>
          </Link>
          <button
            type="button"
            onClick={() => setSubmitted(false)}
            className="block w-full text-center text-sm text-ink-500 hover:text-ink-800"
          >
            Try a different email
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <h2 className="font-display text-3xl text-ink-900">Forgot your password?</h2>
      <p className="mt-2 text-sm text-ink-500">
        Enter the email you used to sign up and we&apos;ll send you a link to
        reset it.
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

        <Button
          type="submit"
          size="lg"
          isLoading={isSubmitting}
          className="w-full"
        >
          Send reset link
        </Button>
      </form>

      <Link
        to={ROUTES.LOGIN}
        className="mt-8 inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-800"
      >
        <ArrowLeft size={14} />
        Back to sign in
      </Link>
    </div>
  );
};

export default ForgotPassword;
