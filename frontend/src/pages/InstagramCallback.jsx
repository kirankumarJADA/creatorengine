import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { CheckCircle2, XCircle, Loader2, ArrowRight } from 'lucide-react';

import Button from '../components/form/Button.jsx';
import { useInstagramStore } from '../store/instagramStore.js';
import { ROUTES } from '../utils/constants.js';

/**
 * Landing page after the Meta OAuth dance.
 *
 * Flow:
 *   1. Backend redirects here with ?status=success | ?status=error&message=...
 *   2. We refresh the connection state from /api/instagram/status so any
 *      Settings page subsequently opened reads the freshest data.
 *   3. After a short pause we auto-redirect to /settings.
 *
 * The auto-redirect timer can be skipped via the "Go now" button. We
 * keep the page on screen for ~1.5s of either state so the user
 * actually sees the result instead of a flash.
 */
const InstagramCallback = () => {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const fetchStatus = useInstagramStore((s) => s.fetchStatus);

  const status  = params.get('status'); // 'success' | 'error' | null
  const message = params.get('message');

  // While we don't know yet (rare race), show a spinner.
  const [phase, setPhase] = useState(
    status === 'success' ? 'success' :
    status === 'error'   ? 'error'   : 'loading'
  );

  // Refresh server-side status when we arrive in 'success' state
  useEffect(() => {
    if (phase === 'success') {
      fetchStatus();
    }
  }, [phase, fetchStatus]);

  // Auto-bounce to Settings after a beat
  useEffect(() => {
    if (phase === 'loading') return;
    const t = setTimeout(() => navigate(ROUTES.SETTINGS), 2500);
    return () => clearTimeout(t);
  }, [phase, navigate]);

  return (
    <div className="grid min-h-screen place-items-center bg-ink-50 px-6 py-12 dark:bg-ink-950">
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="card w-full max-w-md p-8 text-center"
      >
        {phase === 'loading' && <LoadingState />}
        {phase === 'success' && <SuccessState />}
        {phase === 'error'   && <ErrorState message={message} />}

        <Button
          rightIcon={ArrowRight}
          onClick={() => navigate(ROUTES.SETTINGS)}
          className="mt-6"
          variant={phase === 'error' ? 'secondary' : 'primary'}
        >
          {phase === 'error' ? 'Back to Settings' : 'Go now'}
        </Button>
      </motion.div>
    </div>
  );
};

const LoadingState = () => (
  <>
    <Loader2 size={32} className="mx-auto animate-spin text-ink-400" />
    <h1 className="mt-4 text-xl font-semibold text-ink-900 dark:text-ink-100">
      Finishing up…
    </h1>
    <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
      Hold tight — we&apos;re confirming your Instagram connection.
    </p>
  </>
);

const SuccessState = () => (
  <>
    <span className="grid h-14 w-14 mx-auto place-items-center rounded-2xl bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400">
      <CheckCircle2 size={28} strokeWidth={2.25} />
    </span>
    <h1 className="mt-4 text-xl font-semibold text-ink-900 dark:text-ink-100">
      You&apos;re connected
    </h1>
    <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
      Your Instagram Business account is linked to CreatorEngine.
      Heading back to Settings…
    </p>
  </>
);

const ErrorState = ({ message }) => (
  <>
    <span className="grid h-14 w-14 mx-auto place-items-center rounded-2xl bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-400">
      <XCircle size={28} strokeWidth={2.25} />
    </span>
    <h1 className="mt-4 text-xl font-semibold text-ink-900 dark:text-ink-100">
      Connection failed
    </h1>
    <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
      {message
        ? decodeURIComponent(message)
        : 'Something went wrong during the Instagram OAuth flow. Please try again.'}
    </p>
  </>
);

export default InstagramCallback;
