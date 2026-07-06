import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Mail, User, ArrowRight, ShieldCheck } from 'lucide-react';
import toast from 'react-hot-toast';
import { GoogleAuthProvider, signInWithPopup } from 'firebase/auth';

import FormField     from '../components/form/FormField.jsx';
import PasswordField from '../components/form/PasswordField.jsx';
import Button        from '../components/form/Button.jsx';

import { useAuthStore } from '../store/authStore.js';
import { ROUTES }       from '../utils/constants.js';
import { EMAIL_RULES, PASSWORD_RULES, NAME_RULES } from '../utils/validators.js';
import authService from '../services/authService.js';
import { auth } from '../firebase.js';

const ALLOWED_EMAIL_DOMAINS = [
  'gmail.com', 'googlemail.com',
  'outlook.com', 'hotmail.com', 'live.com',
];

const isAllowedEmailDomain = (email) => {
  const domain = String(email || '').split('@')[1]?.toLowerCase().trim();
  return !!domain && ALLOWED_EMAIL_DOMAINS.includes(domain);
};

const Register = () => {
  const navigate = useNavigate();
  const persistSession = useAuthStore((s) => s._persistSession);
  const [step, setStep] = useState('form'); // 'form' | 'otp'
  const [submitting, setSubmitting] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const [pendingValues, setPendingValues] = useState(null);
  const [otp, setOtp] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
    getValues,
  } = useForm({
    defaultValues: { name: '', email: '', password: '', confirmPassword: '' },
    mode: 'onTouched',
  });

  const onSubmit = async (values) => {
    if (!isAllowedEmailDomain(values.email)) {
      toast.error('Please use a Gmail, Outlook, or Hotmail address.');
      return;
    }
    if (values.password !== values.confirmPassword) {
      toast.error('Passwords do not match.');
      return;
    }

    setSubmitting(true);
    try {
      await authService.sendOtp({ email: values.email });
      setPendingValues(values);
      setStep('otp');
      toast.success('Verification code sent to your email.');
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Could not send code. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const onVerifyOtp = async () => {
    if (!otp || otp.length < 4) {
      toast.error('Enter the verification code.');
      return;
    }

    setSubmitting(true);
    try {
      const data = await authService.verifyOtp({
        email: pendingValues.email,
        otp,
        name: pendingValues.name,
        password: pendingValues.password,
      });
      persistSession(data);
      toast.success('Account created!');
      navigate(ROUTES.DASHBOARD, { replace: true });
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Invalid or expired code.');
    } finally {
      setSubmitting(false);
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
      navigate(ROUTES.DASHBOARD, { replace: true });
    } catch (err) {
      if (err?.code !== 'auth/popup-closed-by-user') {
        toast.error('Google sign-in failed. Please try again.');
      }
    } finally {
      setGoogleLoading(false);
    }
  };

  if (step === 'otp') {
    return (
      <div>
        <h2 className="font-display text-3xl text-ink-900">Verify your email</h2>
        <p className="mt-2 text-sm text-ink-500">
          Enter the code we sent to {pendingValues?.email}.
        </p>

        <div className="mt-8 space-y-4">
          <input
            type="text"
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
            placeholder="Enter code"
            className="w-full rounded-lg border border-ink-200 px-4 py-3 text-sm"
          />
          <Button
            type="button"
            size="lg"
            isLoading={submitting}
            rightIcon={ShieldCheck}
            className="w-full"
            onClick={onVerifyOtp}
          >
            Verify & Create Account
          </Button>
        </div>
      </div>
    );
  }

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
          type="text"
          placeholder="Your name"
          leftIcon={User}
          error={errors.name?.message}
          {...register('name', NAME_RULES)}
        />

        <FormField
          label="Email"
          type="email"
          placeholder="you@company.com"
          leftIcon={Mail}
          error={errors.email?.message}
          {...register('email', EMAIL_RULES)}
        />

        <PasswordField
          label="Password"
          placeholder="••••••••"
          error={errors.password?.message}
          {...register('password', PASSWORD_RULES)}
        />

        <PasswordField
          label="Confirm password"
          placeholder="••••••••"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword', {
            validate: (value) =>
              value === getValues('password') || 'Passwords do not match',
          })}
        />

        <Button
          type="submit"
          size="lg"
          isLoading={submitting}
          rightIcon={ArrowRight}
          className="w-full"
        >
          Continue
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