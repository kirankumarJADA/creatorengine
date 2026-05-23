import { cn } from '../../utils/helpers.js';

const PageHeader = ({ title, description, actions, className }) => (
  <div
    className={cn(
      'mb-6 flex flex-col gap-3 sm:mb-8 sm:flex-row sm:items-center sm:justify-between',
      className
    )}
  >
    <div>
      <h1 className="text-2xl font-semibold tracking-tight text-ink-900 dark:text-ink-100 sm:text-3xl">
        {title}
      </h1>
      {description && (
        <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">{description}</p>
      )}
    </div>
    {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
  </div>
);

export default PageHeader;
