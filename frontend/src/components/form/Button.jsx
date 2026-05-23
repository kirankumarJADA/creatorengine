import { forwardRef } from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '../../utils/helpers.js';

/**
 * Form-friendly button with loading state.
 *
 * Variants: primary | secondary | ghost
 * Sizes:    sm | md | lg
 */
const VARIANTS = {
  primary:
    'bg-brand-600 text-white hover:bg-brand-700 active:bg-brand-800 disabled:bg-brand-300',
  secondary:
    'bg-white text-ink-900 border border-ink-200 hover:bg-ink-50 disabled:opacity-60',
  ghost:
    'text-ink-700 hover:bg-ink-100 disabled:opacity-60',
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
          'inline-flex items-center justify-center gap-2 font-semibold transition-all duration-150',
          'disabled:cursor-not-allowed',
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
