import { forwardRef } from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '../../utils/helpers.js';

/**
 * Form-friendly button with loading state.
 * Variants: primary | secondary | ghost
 * Sizes:    sm | md | lg
 */
const VARIANTS = {
  primary:
    'bg-brand-600 text-white shadow-soft hover:bg-brand-700 hover:shadow-elevated hover:-translate-y-px ' +
    'active:translate-y-0 active:bg-brand-800 active:shadow-soft disabled:bg-brand-300 disabled:shadow-none disabled:translate-y-0',
  secondary:
    'bg-white text-ink-900 border border-ink-200 shadow-soft hover:bg-ink-50 hover:-translate-y-px hover:shadow-elevated ' +
    'active:translate-y-0 disabled:opacity-60 disabled:translate-y-0 ' +
    'dark:bg-ink-900 dark:text-ink-100 dark:border-ink-800 dark:hover:bg-ink-800',
  ghost:
    'text-ink-700 hover:bg-ink-100 disabled:opacity-60 dark:text-ink-300 dark:hover:bg-ink-800',
};

const SIZES = {
  sm: 'px-3 py-1.5 text-xs rounded-lg',
  md: 'px-4 py-2.5 text-sm rounded-xl',
  lg: 'px-5 py-3 text-base rounded-xl',
};

const Button = forwardRef(
  (
    {
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon: LeftIcon,
      rightIcon: RightIcon,
      className,
      type = 'button',
      disabled,
      children,
      ...rest
    },
    ref
  ) => {
    return (
      <button
        ref={ref}
        type={type}
        disabled={disabled || isLoading}
        className={cn(
          'inline-flex items-center justify-center gap-2 font-semibold transition-all duration-150 will-change-transform',
          'disabled:cursor-not-allowed',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/40 focus-visible:ring-offset-2',
          'focus-visible:ring-offset-white dark:focus-visible:ring-offset-ink-950',
          VARIANTS[variant],
          SIZES[size],
          className
        )}
        {...rest}
      >
        {isLoading ? (
          <Loader2 size={16} className="animate-spin" />
        ) : (
          LeftIcon && <LeftIcon size={16} />
        )}
        {children}
        {!isLoading && RightIcon && <RightIcon size={16} />}
      </button>
    );
  }
);

Button.displayName = 'Button';
export default Button;