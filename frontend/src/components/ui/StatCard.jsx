import { TrendingUp } from 'lucide-react';
import { motion } from 'framer-motion';
import { cn } from '../../utils/helpers.js';

const TONES = {
  brand:   'bg-brand-100 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300',
  success: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400',
  warning: 'bg-amber-100 text-amber-800 dark:bg-amber-500/10 dark:text-amber-400',
  neutral: 'bg-ink-100 text-ink-700 dark:bg-ink-800 dark:text-ink-300',
};

const StatCard = ({
  label, value, delta, icon: Icon, tone = 'neutral', index = 0,
}) => (
  <motion.div
    initial={{ opacity: 0, y: 8 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ duration: 0.3, delay: index * 0.05 }}
    className="card p-5 sm:p-6"
  >
    <div className="flex items-start justify-between">
      {Icon && (
        <span className={cn('grid h-10 w-10 place-items-center rounded-xl', TONES[tone])}>
          <Icon size={18} />
        </span>
      )}
      {delta && (
        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400">
          <TrendingUp size={12} />
          {delta}
        </span>
      )}
    </div>
    <p className="mt-4 text-3xl font-semibold tracking-tight text-ink-900 dark:text-ink-100">
      {typeof value === 'number' ? value.toLocaleString() : value}
    </p>
    <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">{label}</p>
  </motion.div>
);

export default StatCard;
