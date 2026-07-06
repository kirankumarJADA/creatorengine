import { Outlet } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Rocket, Zap, Users, ShieldCheck, Send, MessageSquare } from 'lucide-react';

/**
 * Premium split-panel auth layout.
 * Left: brand narrative with layered depth (gradient mesh, dot grid,
 *       floating product preview). Right: clean form canvas.
 * Uses only existing tokens (ink / brand / font-display / shadow-*).
 */

const FEATURES = [
  { icon: Rocket,      label: 'Launch campaigns in minutes' },
  { icon: Users,       label: 'Grow your audience on autopilot' },
  { icon: ShieldCheck, label: 'Enterprise-grade security' },
];

const LogoMark = () => (
  <div className="flex items-center gap-3">
    {/* Swap this span for your existing <img src=... /> logo */}
    <span className="grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 shadow-elevated">
      <Rocket size={20} className="text-white" />
    </span>
    <span className="text-lg font-semibold tracking-tight text-white">
      CreatorEngine
    </span>
  </div>
);

/* Floating mini product-preview card — pure decoration, no data */
const PreviewCard = () => (
  <motion.div
    initial={{ opacity: 0, y: 24 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ delay: 0.35, duration: 0.6, ease: [0.21, 0.47, 0.32, 0.98] }}
    className="relative mt-12 hidden xl:block"
  >
    <motion.div
      animate={{ y: [0, -8, 0] }}
      transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
      className="w-[340px] rounded-2xl border border-white/10 bg-white/[0.06] p-5 backdrop-blur-xl shadow-elevated"
    >
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium uppercase tracking-wider text-ink-400">
          Live automation
        </p>
        <span className="flex items-center gap-1.5 rounded-full bg-emerald-400/10 px-2.5 py-1 text-[11px] font-medium text-emerald-300">
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
          Active
        </span>
      </div>

      <div className="mt-4 flex items-center gap-3">
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-brand-500/20 text-brand-300">
          <MessageSquare size={16} />
        </span>
        <div className="h-px flex-1 bg-gradient-to-r from-brand-400/60 to-transparent" />
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-brand-500/20 text-brand-300">
          <Send size={16} />
        </span>
      </div>

      <p className="mt-3 text-sm text-ink-300">
        Comment <span className="text-white">"link"</span> → DM sent
      </p>

      <div className="mt-4 grid grid-cols-3 gap-3 border-t border-white/10 pt-4">
        {[
          { k: '2.4k', v: 'DMs sent' },
          { k: '98%',  v: 'Delivered' },
          { k: '312',  v: 'New leads' },
        ].map((s) => (
          <div key={s.v}>
            <p className="text-base font-semibold text-white">{s.k}</p>
            <p className="text-[11px] text-ink-400">{s.v}</p>
          </div>
        ))}
      </div>
    </motion.div>

    {/* Small trailing chip */}
    <motion.div
      animate={{ y: [0, 10, 0] }}
      transition={{ duration: 7, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
      className="absolute -right-6 -top-6 flex items-center gap-2 rounded-xl border border-white/10 bg-white/[0.08] px-3 py-2 backdrop-blur-xl"
    >
      <Zap size={13} className="text-amber-300" />
      <span className="text-xs font-medium text-white">Triggered just now</span>
    </motion.div>
  </motion.div>
);

const AuthLayout = () => (
  <div className="flex min-h-screen bg-white dark:bg-ink-950">
    {/* ── Left · brand panel ─────────────────────────── */}
    <div className="relative hidden w-[46%] flex-col justify-between overflow-hidden bg-ink-950 p-10 lg:flex xl:p-14">
      {/* Depth layers */}
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-br from-brand-950 via-ink-950 to-ink-950" />
        <div className="absolute -left-32 top-1/4 h-96 w-96 rounded-full bg-brand-600/20 blur-[120px]" />
        <div className="absolute -bottom-24 right-0 h-80 w-80 rounded-full bg-brand-500/10 blur-[100px]" />
        <div className="bg-dotgrid absolute inset-0 opacity-40" />
        {/* top edge light */}
        <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-brand-400/40 to-transparent" />
      </div>

      <div className="relative">
        <LogoMark />
      </div>

      <div className="relative">
        <motion.h1
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
          className="max-w-md font-display text-4xl leading-[1.15] text-white xl:text-5xl"
        >
          The all-in-one platform for{' '}
          <span className="bg-gradient-to-r from-brand-300 to-brand-400 bg-clip-text text-transparent">
            modern creators
          </span>
          .
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.12, duration: 0.5 }}
          className="mt-4 max-w-sm text-[15px] leading-relaxed text-ink-400"
        >
          Build, automate, and grow your audience — all in one place.
        </motion.p>

        <motion.ul
          initial="hidden"
          animate="visible"
          variants={{ visible: { transition: { staggerChildren: 0.08, delayChildren: 0.2 } } }}
          className="mt-8 space-y-3.5"
        >
          {FEATURES.map(({ icon: Icon, label }) => (
            <motion.li
              key={label}
              variants={{
                hidden:  { opacity: 0, x: -12 },
                visible: { opacity: 1, x: 0 },
              }}
              className="flex items-center gap-3.5"
            >
              <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-white/10 bg-white/[0.06] text-brand-300">
                <Icon size={16} />
              </span>
              <span className="text-sm text-ink-200">{label}</span>
            </motion.li>
          ))}
        </motion.ul>

        <PreviewCard />
      </div>

      <p className="relative text-xs text-ink-500">
        © 2026 CreatorEngine. All rights reserved.
      </p>
    </div>

    {/* ── Right · form canvas ────────────────────────── */}
    <div className="relative flex min-w-0 flex-1 items-center justify-center px-5 py-10 sm:px-10">
      {/* faint top glow so the white panel isn't sterile */}
      <div className="pointer-events-none absolute inset-x-0 top-0 h-40 bg-gradient-to-b from-brand-50/70 to-transparent dark:from-brand-950/20" />

      <motion.div
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: [0.21, 0.47, 0.32, 0.98] }}
        className="relative w-full max-w-[420px]"
      >
        <Outlet />
      </motion.div>
    </div>
  </div>
);

export default AuthLayout;