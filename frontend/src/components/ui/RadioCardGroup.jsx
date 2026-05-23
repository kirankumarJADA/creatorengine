import { Check } from 'lucide-react';
import { motion } from 'framer-motion';
import { cn } from '../../utils/helpers.js';

/**
 * Big "select one of these tiles" pattern — used for the Trigger and
 * Action steps of the builder. Tiles are full keyboard-accessible
 * radios under the hood.
 *
 * Each option:
 *   {
 *     value:       string,
 *     label:       string,
 *     description: string?,
 *     icon:        LucideIcon?,
 *     tone:        'brand' | 'success' | 'warning' | 'neutral'  (chip color)
 *     disabled:    boolean?,
 *   }
 */
const TONE_CHIP = {
  brand:   'bg-brand-100 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300',
  success: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400',
  warning: 'bg-amber-100 text-amber-800 dark:bg-amber-500/10 dark:text-amber-400',
  neutral: 'bg-ink-100 text-ink-700 dark:bg-ink-800 dark:text-ink-300',
};

const RadioCardGroup = ({
  name,
  options = [],
  value,
  onChange,
  columns = 3,
  className,
}) => {
  const colClass = {
    1: 'grid-cols-1',
    2: 'grid-cols-1 sm:grid-cols-2',
    3: 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3',
  }[columns] || 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3';

  return (
    <div role="radiogroup" className={cn('grid gap-3', colClass, className)}>
      {options.map((opt) => {
        const selected = opt.value === value;
        const Icon = opt.icon;
        return (
          <motion.label
            key={opt.value}
            whileTap={{ scale: 0.985 }}
            className={cn(
              'group relative flex h-full cursor-pointer flex-col rounded-2xl border p-4 transition-colors',
              opt.disabled && 'cursor-not-allowed opacity-60',
              selected
                ? 'border-brand-500 bg-brand-50/60 ring-2 ring-brand-500/40 dark:border-brand-400 dark:bg-brand-500/10 dark:ring-brand-400/30'
                : 'border-ink-200 bg-white hover:border-ink-300 dark:border-ink-800 dark:bg-ink-900 dark:hover:border-ink-700'
            )}
          >
            <input
              type="radio"
              name={name}
              value={opt.value}
              checked={selected}
              onChange={() => !opt.disabled && onChange?.(opt.value)}
              disabled={opt.disabled}
              className="sr-only"
            />

            <div className="flex items-start gap-3">
              {Icon && (
                <span
                  className={cn(
                    'grid h-10 w-10 shrink-0 place-items-center rounded-xl',
                    TONE_CHIP[opt.tone || 'neutral']
                  )}
                >
                  <Icon size={18} />
                </span>
              )}
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-ink-900 dark:text-ink-100">
                  {opt.label}
                </p>
                {opt.description && (
                  <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
                    {opt.description}
                  </p>
                )}
              </div>
              <span
                className={cn(
                  'mt-0.5 grid h-5 w-5 shrink-0 place-items-center rounded-full border transition-colors',
                  selected
                    ? 'border-brand-600 bg-brand-600 text-white'
                    : 'border-ink-300 bg-white dark:border-ink-700 dark:bg-ink-900'
                )}
              >
                {selected && <Check size={12} strokeWidth={3} />}
              </span>
            </div>
          </motion.label>
        );
      })}
    </div>
  );
};

export default RadioCardGroup;
