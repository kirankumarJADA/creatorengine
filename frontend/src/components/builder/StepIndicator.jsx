import { Check } from 'lucide-react';
import { motion } from 'framer-motion';
import { cn } from '../../utils/helpers.js';
import { STEPS } from '../../store/builderStore.js';

/**
 * Top-of-wizard progress strip.
 * Tap a completed (lower-numbered) step to jump back to it.
 */
const StepIndicator = ({ current, onStepClick }) => (
  <ol className="flex items-center gap-2 overflow-x-auto pb-1 sm:gap-3">
    {STEPS.map((step, idx) => {
      const status =
        step.id < current ? 'done' : step.id === current ? 'active' : 'upcoming';
      const isLast = idx === STEPS.length - 1;
      const canJump = status === 'done';

      return (
        <li key={step.id} className="flex items-center gap-2 sm:gap-3">
          <button
            type="button"
            onClick={() => canJump && onStepClick?.(step.id)}
            disabled={!canJump && status !== 'active'}
            className={cn(
              'group flex items-center gap-2 rounded-xl px-2 py-1 transition-colors',
              canJump && 'hover:bg-ink-100 dark:hover:bg-ink-800/60',
              status === 'active' && 'bg-ink-100/70 dark:bg-ink-800/60',
              status === 'upcoming' && 'cursor-default'
            )}
          >
            <span
              className={cn(
                'grid h-7 w-7 shrink-0 place-items-center rounded-full text-xs font-semibold transition-colors',
                status === 'done'    && 'bg-brand-600 text-white',
                status === 'active'  && 'bg-ink-900 text-white dark:bg-brand-600',
                status === 'upcoming'&& 'bg-ink-200 text-ink-500 dark:bg-ink-800 dark:text-ink-400'
              )}
            >
              {status === 'done' ? <Check size={14} strokeWidth={3} /> : step.id}
            </span>
            <span
              className={cn(
                'hidden text-sm font-medium sm:inline',
                status === 'upcoming'
                  ? 'text-ink-400 dark:text-ink-500'
                  : 'text-ink-900 dark:text-ink-100'
              )}
            >
              {step.label}
            </span>
          </button>
          {!isLast && (
            <span
              aria-hidden="true"
              className={cn(
                'h-px w-6 sm:w-10',
                status === 'done'
                  ? 'bg-brand-500'
                  : 'bg-ink-200 dark:bg-ink-800'
              )}
            />
          )}
        </li>
      );
    })}
  </ol>
);

export default StepIndicator;
