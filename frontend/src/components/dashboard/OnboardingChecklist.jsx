import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Check, Circle, Instagram, Workflow, MessageCircle, ArrowRight } from 'lucide-react';
import { Card } from '../ui/Card.jsx';
import instagramService from '../../services/instagramService.js';
import { ROUTES } from '../../utils/constants.js';
import { cn } from '../../utils/helpers.js';

/**
 * 3-step onboarding checklist for new beta users. Auto-hides once all
 * three steps are complete, so established users never see it.
 *
 * Step completion is derived from real signals, not local state:
 *   1. Instagram connected — igStatus has a username
 *   2. First automation built — at least 1 in the user's list
 *   3. First automation tested — at least 1 execution log row
 */
const OnboardingChecklist = ({ igConnected, hasAutomations, hasActivity }) => {
  const allDone = igConnected && hasAutomations && hasActivity;
  if (allDone) return null;

  const startConnect = async () => {
    try {
      const { authUrl } = await instagramService.startConnect();
      if (authUrl) window.location.href = authUrl;
    } catch {
      // Errors are surfaced by the axios interceptor; nothing to do here.
    }
  };

  const steps = [
    {
      key:         'ig',
      icon:        Instagram,
      title:       'Connect your Instagram',
      description: 'Link your Business or Creator account so CreatorEngine can read comments and send DMs.',
      done:        igConnected,
      cta:         igConnected ? null : { label: 'Connect Instagram', onClick: startConnect },
    },
    {
      key:         'automation',
      icon:        Workflow,
      title:       'Create your first automation',
      description: 'Pick a trigger (comment, DM, story reply) and an action (send DM, share a link).',
      done:        hasAutomations,
      cta:         hasAutomations ? null : { label: 'Build automation', to: ROUTES.AUTOMATION_NEW },
    },
    {
      key:         'test',
      icon:        MessageCircle,
      title:       'Test it live',
      description: 'From another Instagram account, comment your trigger word on the right post. You should get a DM in seconds.',
      done:        hasActivity,
      cta:         null,
    },
  ];

  const completedCount = steps.filter((s) => s.done).length;

  return (
    <Card className="mb-6 border-brand-200 bg-gradient-to-br from-brand-50 to-transparent dark:border-brand-500/20 dark:from-brand-500/5">
      <div className="mb-5 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-brand-700 dark:text-brand-400">
            Get started
          </p>
          <h3 className="mt-1 text-lg font-semibold text-ink-900 dark:text-ink-100">
            Welcome to CreatorEngine
          </h3>
          <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
            Three steps to your first automated DM.
          </p>
        </div>
        <div className="text-right">
          <p className="text-2xl font-semibold text-ink-900 dark:text-ink-100">
            {completedCount}<span className="text-ink-400 dark:text-ink-500">/3</span>
          </p>
          <p className="text-xs text-ink-500 dark:text-ink-400">complete</p>
        </div>
      </div>

      <ol className="space-y-2">
        {steps.map((step, i) => (
          <motion.li
            key={step.key}
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.05 }}
          >
            <StepRow step={step} />
          </motion.li>
        ))}
      </ol>
    </Card>
  );
};

const StepRow = ({ step }) => {
  const { icon: Icon, title, description, done, cta } = step;

  return (
    <div
      className={cn(
        'flex items-start gap-3 rounded-xl border p-3 transition-colors',
        done
          ? 'border-emerald-200 bg-emerald-50/50 dark:border-emerald-500/20 dark:bg-emerald-500/5'
          : 'border-ink-100 bg-white dark:border-ink-800 dark:bg-ink-900/40'
      )}
    >
      <span
        className={cn(
          'grid h-9 w-9 shrink-0 place-items-center rounded-xl',
          done
            ? 'bg-emerald-500 text-white'
            : 'bg-brand-100 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
        )}
      >
        {done ? <Check size={16} /> : <Icon size={16} />}
      </span>

      <div className="min-w-0 flex-1">
        <p
          className={cn(
            'text-sm font-semibold',
            done
              ? 'text-emerald-800 line-through decoration-emerald-400/60 dark:text-emerald-200'
              : 'text-ink-900 dark:text-ink-100'
          )}
        >
          {title}
        </p>
        <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">{description}</p>
      </div>

      {cta && !done && (
        cta.to ? (
          <Link
            to={cta.to}
            className="inline-flex shrink-0 items-center gap-1 self-center rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-700"
          >
            {cta.label}
            <ArrowRight size={12} />
          </Link>
        ) : (
          <button
            type="button"
            onClick={cta.onClick}
            className="inline-flex shrink-0 items-center gap-1 self-center rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-brand-700"
          >
            {cta.label}
            <ArrowRight size={12} />
          </button>
        )
      )}

      {done && (
        <span className="self-center text-xs font-medium text-emerald-700 dark:text-emerald-400">
          Done
        </span>
      )}
    </div>
  );
};

export default OnboardingChecklist;