import { forwardRef } from 'react';
import { cn } from '../../utils/helpers.js';

const IconButton = forwardRef(
  ({ children, className, variant = 'ghost', size = 'md', tone, ...rest }, ref) => {
    const sizes = { sm: 'h-8 w-8', md: 'h-9 w-9', lg: 'h-10 w-10' };
    const variants = {
      ghost:
        'text-ink-600 hover:bg-ink-100 dark:text-ink-300 dark:hover:bg-ink-800',
      outline:
        'border border-ink-200 bg-white text-ink-700 hover:bg-ink-50 dark:border-ink-800 dark:bg-ink-900 dark:text-ink-200 dark:hover:bg-ink-800/60',
      danger:
        'text-ink-500 hover:bg-red-50 hover:text-red-600 dark:text-ink-400 dark:hover:bg-red-500/10',
    };
    return (
      <button
        ref={ref}
        type="button"
        className={cn(
          'inline-flex shrink-0 items-center justify-center rounded-lg transition-colors',
          'disabled:cursor-not-allowed disabled:opacity-50',
          sizes[size],
          variants[tone || variant],
          className
        )}
        {...rest}
      >
        {children}
      </button>
    );
  }
);

IconButton.displayName = 'IconButton';
export default IconButton;
