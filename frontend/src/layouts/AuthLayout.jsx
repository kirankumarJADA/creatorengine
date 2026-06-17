import { Outlet, Link } from 'react-router-dom';
import { ShieldCheck, Rocket, Users } from 'lucide-react';
import { APP_NAME } from '../utils/constants.js';

/**
 * Split-screen layout shared by Login, Register, and ForgotPassword.
 * Left pane = brand/marketing. Right pane = form.
 */
const AuthLayout = () => {
  return (
    <div className="min-h-screen bg-white lg:grid lg:grid-cols-2">
      {/* ─── Left: brand pane ──────────────────────────── */}
      <aside className="relative hidden overflow-hidden bg-ink-950 lg:flex lg:flex-col lg:justify-between lg:p-12 lg:text-white">
        {/* Decorative atmosphere */}
        <div className="absolute inset-0 bg-dotgrid opacity-[0.15]" />
        <div className="absolute -top-32 -right-32 h-96 w-96 rounded-full bg-brand-600 opacity-30 blur-3xl" />
        <div className="absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-brand-800 opacity-30 blur-3xl" />

        {/* Logo */}
        <Link to="/" className="relative z-10 inline-flex items-center gap-2.5">
          <img
            src="/logo-mark.png"
            alt={APP_NAME}
            className="h-9 w-9 shrink-0 object-contain"
          />
          <span className="text-lg font-semibold tracking-tight">
            {APP_NAME}
          </span>
        </Link>

        {/* Pitch */}
        <div className="relative z-10 max-w-md animate-fade-in">
          <h1 className="font-display text-4xl leading-tight text-white">
            The all-in-one platform for{' '}
            <em className="not-italic text-brand-300">modern creators</em>.
          </h1>
          <p className="mt-4 text-ink-300">
            Build, automate, and grow your audience — all in one place.
          </p>

          <ul className="mt-10 space-y-4">
            <Feature icon={Rocket} text="Launch campaigns in minutes" />
            <Feature icon={Users} text="Grow your audience on autopilot" />
            <Feature icon={ShieldCheck} text="Enterprise-grade security" />
          </ul>
        </div>

        <p className="relative z-10 text-xs text-ink-400">
          © {new Date().getFullYear()} {APP_NAME}. All rights reserved.
        </p>
      </aside>

      {/* ─── Right: form pane ──────────────────────────── */}
      <main className="flex min-h-screen items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-md animate-fade-in">
          <Outlet />
        </div>
      </main>
    </div>
  );
};

const Feature = ({ icon: Icon, text }) => (
  <li className="flex items-center gap-3 text-sm text-ink-200">
    <span className="grid h-8 w-8 place-items-center rounded-lg bg-white/10">
      <Icon size={16} />
    </span>
    {text}
  </li>
);

export default AuthLayout;