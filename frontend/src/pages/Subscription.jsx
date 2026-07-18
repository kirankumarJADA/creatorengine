import { useEffect, useState } from 'react';
import { Check, Sparkles, Lock } from 'lucide-react';

import PageHeader from '../components/ui/PageHeader.jsx';
import Button from '../components/form/Button.jsx';
import instagramService from '../services/instagramService.js';
import { cn } from '../utils/helpers.js';

const feature = (label, comingSoon = false) => ({ label, comingSoon });

const PLANS = [
  {
    id: 'FREE',
    name: 'Free',
    priceLabel: '$0',
    period: '/mo',
    features: [
      feature('Up to 2 Instagram accounts'),
      feature('DM & comment automations'),
      feature('Ice breakers'),
      feature('Follow-up messages'),
      feature('Email & contact collection'),
    ],
  },
  {
    id: 'PRO',
    name: 'Pro',
    priceLabel: 'Price announced at launch',
    period: '',
    highlight: true,
    comingSoon: true,
    features: [
      feature('Everything in Free'),
      feature('Up to 5 Instagram accounts'),
      feature('Bot protection'),
      feature('AI FAQ (AI-powered DM answers)'),
      feature('AI Autopilot / FAQ Builder', true),
      feature('AI product recommendations', true),
      feature('AI lead qualification', true),
      feature('AI conversation summaries', true),
      feature('Priority support'),
    ],
  },
];

const Subscription = () => {
  const [plan, setPlan] = useState('FREE');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const limits = await instagramService.getPlanLimits();
        setPlan(limits?.plan || 'FREE');
      } catch {
        setPlan('FREE');
      } finally {
        setIsLoading(false);
      }
    })();
  }, []);

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        title="Subscription"
        description="Manage your plan. Pro is launching soon — some AI features below are still in progress."
      />

      <div className="grid gap-4 sm:grid-cols-2">
        {PLANS.map((p) => {
          const isCurrent = !isLoading && plan === p.id;
          return (
            <div
              key={p.id}
              className={cn(
                'card relative flex flex-col p-6',
                p.highlight && 'border-2 border-brand-300 dark:border-brand-600'
              )}
            >
              {p.highlight && (
                <span className="absolute -top-3 left-6 rounded-full bg-brand-600 px-3 py-1 text-xs font-semibold text-white shadow-soft">
                  {p.comingSoon ? 'Launching Soon' : 'Recommended'}
                </span>
              )}

              <div className="mb-4 flex items-center gap-2">
                {p.id === 'PRO' && <Sparkles size={18} className="text-brand-600 dark:text-brand-400" />}
                <h3 className="text-lg font-semibold text-ink-900 dark:text-ink-100">{p.name}</h3>
                {isCurrent && (
                  <span className="ml-auto rounded-full bg-ink-100 px-2.5 py-0.5 text-xs font-medium text-ink-600 dark:bg-ink-800 dark:text-ink-300">
                    Current plan
                  </span>
                )}
              </div>

              <div className="mb-5">
                {p.period ? (
                  <>
                    <span className="text-3xl font-bold text-ink-900 dark:text-ink-100">{p.priceLabel}</span>
                    <span className="text-sm text-ink-500 dark:text-ink-400">{p.period}</span>
                  </>
                ) : (
                  <span className="text-base font-semibold text-ink-500 dark:text-ink-400">{p.priceLabel}</span>
                )}
              </div>

              <ul className="mb-6 flex-1 space-y-2.5">
                {p.features.map((f) => (
                  <li key={f.label} className="flex items-start gap-2 text-sm text-ink-700 dark:text-ink-300">
                    <Check size={16} className="mt-0.5 shrink-0 text-emerald-500" />
                    <span className="flex-1">{f.label}</span>
                    {f.comingSoon && (
                      <span className="shrink-0 rounded-full bg-ink-100 px-2 py-0.5 text-[10px] font-medium text-ink-500 dark:bg-ink-800 dark:text-ink-400">
                        Coming Soon
                      </span>
                    )}
                  </li>
                ))}
              </ul>

              {p.comingSoon ? (
                <Button variant="secondary" disabled leftIcon={Lock} className="w-full">
                  Launching Soon
                </Button>
              ) : (
                <Button variant={isCurrent ? 'secondary' : 'primary'} disabled={isCurrent} className="w-full">
                  {isCurrent ? 'Your current plan' : 'Downgrade'}
                </Button>
              )}
            </div>
          );
        })}
      </div>

      <p className="mt-6 text-center text-xs text-ink-400 dark:text-ink-500">
        Payments aren't live yet. Pro will unlock AI FAQ, bot protection, and more Instagram accounts
        once billing is available — features marked "Coming Soon" are still being built.
      </p>
    </div>
  );
};

export default Subscription;
