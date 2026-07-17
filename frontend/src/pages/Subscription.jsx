import { useEffect, useState } from 'react';
import { Check, Sparkles, Lock } from 'lucide-react';

import PageHeader from '../components/ui/PageHeader.jsx';
import Button from '../components/form/Button.jsx';
import instagramService from '../services/instagramService.js';
import { cn } from '../utils/helpers.js';

const PLANS = [
  {
    id: 'FREE',
    name: 'Free',
    price: '$0',
    period: '/mo',
    features: [
      'Up to 2 Instagram accounts',
      'DM & comment automations',
      'Ice breakers',
      'Follow-up messages',
      'Email & contact collection',
    ],
  },
  {
    id: 'PRO',
    name: 'Pro',
    price: '$—',
    period: '/mo',
    highlight: true,
    comingSoon: true,
    features: [
      'Everything in Free',
      'Up to 10 Instagram accounts',
      'AI FAQ (Gemini-powered DM answers)',
      'AI Autopilot / FAQ Builder',
      'Automated AI conversations',
      'Priority support',
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
        description="Manage your plan. AI features are rolling out soon — join the waitlist by upgrading when it opens."
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
                  {p.comingSoon ? 'Coming Soon' : 'Recommended'}
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
                <span className="text-3xl font-bold text-ink-900 dark:text-ink-100">{p.price}</span>
                <span className="text-sm text-ink-500 dark:text-ink-400">{p.period}</span>
              </div>

              <ul className="mb-6 flex-1 space-y-2.5">
                {p.features.map((f) => (
                  <li key={f} className="flex items-start gap-2 text-sm text-ink-700 dark:text-ink-300">
                    <Check size={16} className="mt-0.5 shrink-0 text-emerald-500" />
                    <span>{f}</span>
                  </li>
                ))}
              </ul>

              {p.comingSoon ? (
                <Button variant="secondary" disabled leftIcon={Lock} className="w-full">
                  Coming Soon
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
        Payments aren't live yet. Pro will unlock AI FAQ, AI Autopilot, and automated conversations
        once billing is available.
      </p>
    </div>
  );
};

export default Subscription;
