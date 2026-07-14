import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Mail, User, ArrowRight, ShieldCheck } from 'lucide-react';
import toast from 'react-hot-toast';
import { GoogleAuthProvider, signInWithPopup } from 'firebase/auth';

import FormField     from '../components/form/FormField.jsx';
import PasswordField from '../components/form/PasswordField.jsx';
import Button        from '../components/form/Button.jsx';
import GoogleButton  from '../components/form/GoogleButton.jsx';

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
      const idToken  = await result.user.getIdToken(true); // force fresh token

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
        <div className="mb-6 grid h-12 w-12 place-items-center rounded-2xl bg-brand-100 shadow-soft dark:bg-brand-500/10">
          <ShieldCheck className="h-6 w-6 text-brand-600 dark:text-brand-400" />
        </div>

        <h2 className="font-display text-[34px] leading-tight text-ink-900 dark:text-ink-100">
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
            <label className="label">Verification code</label>
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
              className="w-full rounded-xl border border-ink-200 bg-white px-4 py-3 text-center
                         text-2xl font-bold tracking-[0.4em] text-ink-900 shadow-soft
                         placeholder:text-ink-300 focus:border-brand-500 focus:outline-none
                         focus:ring-2 focus:ring-brand-500/20
                         dark:border-ink-800 dark:bg-ink-900 dark:text-ink-100"
            />
            {otpError && <p className="error-text">{otpError}</p>}
          </div>

          <Button size="lg" isLoading={verifying} onClick={onVerify} className="w-full">
            Verify &amp; Create Account
          </Button>

          <div className="flex items-center justify-between text-sm">
            <button
              type="button"
              onClick={() => { setStep('form'); setOtp(''); setOtpError(''); }}
              className="text-ink-500 transition-colors hover:text-ink-700 dark:hover:text-ink-300"
            >
              ← Change email
            </button>
            <button
              type="button"
              onClick={onResend}
              disabled={sending}
              className="font-medium text-brand-600 hover:underline disabled:opacity-50 dark:text-brand-400"
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
      <p className="text-xs font-medium uppercase tracking-[0.14em] text-brand-600 dark:text-brand-400">
        Get started
      </p>
      <h2 className="mt-2 font-display text-[34px] leading-tight text-ink-900 dark:text-ink-100">
        Create your account
      </h2>
      <p className="mt-2 text-sm text-ink-500 dark:text-ink-400">
        Start building with CreatorEngine — no credit card required.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-4" noValidate>
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

        <Button type="submit" size="lg" isLoading={sending} rightIcon={ArrowRight} className="w-full">
          Continue
        </Button>

        <p className="text-center text-xs text-ink-500 dark:text-ink-400">
          By creating an account, you agree to our{' '}
          <a href="/terms.html" target="_blank" rel="noreferrer" className="underline underline-offset-2">Terms</a>{' '}
          and{' '}
          <a href="/privacy.html" target="_blank" rel="noreferrer" className="underline underline-offset-2">Privacy Policy</a>.
        </p>
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
        Already have an account?{' '}
        <Link
          to={ROUTES.LOGIN}
          className="font-medium text-ink-900 underline-offset-4 hover:underline dark:text-ink-100"
        >
          Sign in
        </Link>
      </p>
    </div>
  );
};

export default Register;