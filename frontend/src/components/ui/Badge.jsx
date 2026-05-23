import { cn } from '../../utils/helpers.js';

const TONES = {
  neutral: 'bg-ink-100 text-ink-700 dark:bg-ink-800 dark:text-ink-300',
  success: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400',
  warning: 'bg-amber-100 text-amber-800 dark:bg-amber-500/10 dark:text-amber-400',
  danger:  'bg-red-100 text-red-700 dark:bg-red-500/10 dark:text-red-400',
  brand:   'bg-brand-100 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300',
};

const Badge = ({ tone = 'neutral', dot = false, className, children, ...rest }) => {
  const dotTone = {
    neutral: 'bg-ink-400',
    success: 'bg-emerald-500',
    warning: 'bg-amber-500',
    danger:  'bg-red-500',
    brand:   'bg-brand-500',
  }[tone];

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium',
        TONES[tone],
        className
      )}
      {...rest}
    >
      {dot && <span className={cn('h-1.5 w-1.5 rounded-full', dotTone)} />}
      {children}
    </span>
  );
};

export default Badge;
