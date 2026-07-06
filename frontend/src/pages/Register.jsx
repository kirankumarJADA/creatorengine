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
  const navigate       = useNavigate();
  const persistSession = useAuthStore((s) => s._persistSession);

  const [step, setStep]           = useState('form');
  const [sending, setSending]     = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [formData, setFormData]   = useState(null);
  const [otp, setOtp]             = useState('');
  const [otpError, setOtpError]   = useState('');
  const [googleLoading, setGoogleLoading] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    defaultValues: { name: '', email: '', password: '', confirm: '' },
    mode: 'onTouched',
  });

  const password = watch('password');

  const onSubmit = async (payload) => {
    const { confirm: _c, ...rest } = payload;
    setSending(true);
    try {
      await authService.sendOtp({ email: rest.email });
      setFormData(rest);
      setStep('otp');
      toast.success('Verification code sent — check your email!');
    } catch {
      /* interceptor toasted */
    } finally {
      setSending(false);
    }
  };

  const onVerify = async () => {
    if (otp.length !== 6) {
      setOtpError('Enter the 6-digit code from your email.');
      return;
    }
    setOtpError('');
    setVerifying(true);
    try {
      const data = await authService.verifyOtp({
        email:    formData.email,
        otp,
        name:     formData.name,
        password: formData.password,
      });
      persistSession(data);
      toast.success('Account created — welcome to CreatorEngine!');
      navigate(ROUTES.DASHBOARD, { replace: true });
    } catch {
      setOtpError('Invalid or expired code. Please try again.');
    } finally {
      setVerifying(false);
    }
  };

  const onResend = async () => {
    setSending(true);
    try {
      await authService.sendOtp({ email: formData.email });
      toast.success('New code sent!');
      setOtp('');
      setOtpError('');
    } catch {
      /* interceptor toasted */
    } finally {
      setSending(false);
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

  // ── OTP Screen ───────────────────────────────────────────
  if (step === 'otp') {
    return (
      <div>
        <div className="mb-6 flex items-center justify-center w-12 h-12
                        rounded-full bg-brand-100 dark:bg-brand-900">
          <ShieldCheck className="w-6 h-6 text-brand-600 dark:text-brand-400" />
        </div>

        <h2 className="font-display text-3xl text-ink-900 dark:text-ink-100">
          Check your email
        </h2>
        <p className="mt-2 text-sm text-ink-500 dark:text-ink-400">
          We sent a 6-digit code to{' '}
          <span className="font-medium text-ink-900 dark:text-ink-100">
            {formData?.email}
          </span>
        </p>

        <div className="mt-8 space-y-4">
          <div>
            <label className="block text-sm font-medium
                              text-ink-700 dark:text-ink-300 mb-1">
              Verification code
            </label>
            <input
              type="text"
              inputMode="numeric"
              maxLength={6}
              value={otp}
              onChange={(e) => {
                setOtp(e.target.value.replace(/\D/g, '').slice(0, 6));
                setOtpError('');
              }}
              placeholder="000000"
              className="w-full px-4 py-3 text-center text-2xl font-bold
                         tracking-widest rounded-lg border
                         border-ink-200 dark:border-ink-700
                         bg-white dark:bg-ink-900
                         text-ink-900 dark:text-ink-100
                         focus:outline-none focus:ring-2 focus:ring-brand-500
                         placeholder:text-ink-300"
            />
            {otpError && (
              <p className="mt-1 text-sm text-red-500">{otpError}</p>
            )}
          </div>

          <Button
            size="lg"
            isLoading={verifying}
            onClick={onVerify}
            className="w-full"
          >
            Verify &amp; Create Account
          </Button>

          <div className="flex items-center justify-between text-sm">
            <button
              type="button"
              onClick={() => {
                setStep('form');
                setOtp('');
                setOtpError('');
              }}
              className="text-ink-500 hover:text-ink-700
                         dark:hover:text-ink-300"
            >
              ← Change email
            </button>
            <button
              type="button"
              onClick={onResend}
              disabled={sending}
              className="text-brand-600 hover:underline
                         disabled:opacity-50 dark:text-brand-400"
            >
              {sending ? 'Sending…' : 'Resend code'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ── Registration Form ─────────────────────────────────────
  return (
    <div>
      <h2 className="font-display text-3xl text-ink-900 dark:text-ink-100">
        Create your account
      </h2>
      <p className="mt-2 text-sm text-ink-500 dark:text-ink-400">
        Start building with CreatorEngine — no credit card required.
      </p>

      <form onSubmit={handleSubmit(onSubmit)}
            className="mt-8 space-y-4" noValidate>
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
          error={errors.email?.message}
          {...register('email', {
            ...EMAIL_RULES,
            validate: {
              ...(typeof EMAIL_RULES.validate === 'function'
                ? { emailFormat: EMAIL_RULES.validate }
                : EMAIL_RULES.validate),
              allowedDomain: (v) =>
                isAllowedEmailDomain(v) ||
                "This email provider isn't supported. Please use Gmail or Outlook.",
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
          isLoading={sending}
          rightIcon={ArrowRight}
          className="w-full"
        >
          Continue
        </Button>

        <p className="text-center text-xs text-ink-500 dark:text-ink-400">
          By creating an account, you agree to our{' '}
          <a href="/terms.html" target="_blank" rel="noreferrer"
             className="underline">Terms</a>{' '}
          and{' '}
          <a href="/privacy.html" target="_blank" rel="noreferrer"
             className="underline">Privacy Policy</a>.
        </p>
      </form>

      <div className="relative my-6">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-ink-200 dark:border-ink-700" />
        </div>
        <div className="relative flex justify-center text-sm">
          <span className="bg-white dark:bg-ink-900 px-3 text-ink-400">or</span>
        </div>
      </div>

      <button
        type="button"
        onClick={onGoogleSignIn}
        disabled={googleLoading}
        className="w-full flex items-center justify-center gap-3 rounded-lg border
                   border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 px-4 py-3 text-sm font-medium
                   text-ink-700 dark:text-ink-300 shadow-sm hover:bg-ink-50 dark:hover:bg-ink-800 transition
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

      <p className="mt-8 text-center text-sm text-ink-500 dark:text-ink-400">
        Already have an account?{' '}
        <Link to={ROUTES.LOGIN}
              className="font-medium text-ink-900 dark:text-ink-100
                         hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
};

export default Register;