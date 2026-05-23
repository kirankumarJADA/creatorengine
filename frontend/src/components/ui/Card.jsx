import { cn } from '../../utils/helpers.js';

export const Card = ({ className, padded = true, children, ...rest }) => (
  <div
    className={cn(
      'rounded-2xl border bg-white shadow-soft border-ink-100',
      'dark:border-ink-800 dark:bg-ink-900',
      padded && 'p-5 sm:p-6',
      className
    )}
    {...rest}
  >
    {children}
  </div>
);

export const CardHeader = ({ title, description, action, className }) => (
  <div
    className={cn(
      'mb-4 flex items-start justify-between gap-4',
      className
    )}
  >
    <div>
      <h3 className="text-lg font-semibold text-ink-900 dark:text-ink-100">
        {title}
      </h3>
      {description && (
        <p className="mt-0.5 text-sm text-ink-500 dark:text-ink-400">
          {description}
        </p>
      )}
    </div>
    {action}
  </div>
);

export default Card;
