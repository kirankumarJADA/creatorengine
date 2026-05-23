import { cn } from '../../utils/helpers.js';

const EmptyState = ({ icon: Icon, title, description, action, className }) => (
  <div
    className={cn(
      'flex flex-col items-center justify-center rounded-2xl border border-dashed',
      'border-ink-200 bg-white px-6 py-14 text-center',
      'dark:border-ink-800 dark:bg-ink-900',
      className
    )}
  >
    {Icon && (
      <span className="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300">
        <Icon size={22} />
      </span>
    )}
    <h3 className="text-lg font-semibold text-ink-900 dark:text-ink-100">{title}</h3>
    {description && (
      <p className="mt-1 max-w-sm text-sm text-ink-500 dark:text-ink-400">{description}</p>
    )}
    {action && <div className="mt-5">{action}</div>}
  </div>
);

export default EmptyState;
