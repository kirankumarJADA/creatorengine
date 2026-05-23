import { cn } from '../../utils/helpers.js';

const Switch = ({
  checked = false, onChange, disabled = false, size = 'md',
  label, srLabel, className,
}) => {
  const sizes = {
    sm: { track: 'h-5 w-9',  thumb: 'h-3.5 w-3.5', shift: 'translate-x-4' },
    md: { track: 'h-6 w-11', thumb: 'h-4 w-4',     shift: 'translate-x-5' },
  };
  const s = sizes[size] || sizes.md;

  return (
    <label
      className={cn(
        'inline-flex items-center gap-3',
        disabled && 'cursor-not-allowed opacity-60',
        !disabled && 'cursor-pointer',
        className
      )}
    >
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={!label ? srLabel : undefined}
        disabled={disabled}
        onClick={() => !disabled && onChange?.(!checked)}
        className={cn(
          'relative inline-flex shrink-0 items-center rounded-full transition-colors',
          'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/40 focus-visible:ring-offset-2',
          'focus-visible:ring-offset-white dark:focus-visible:ring-offset-ink-900',
          s.track,
          checked ? 'bg-brand-600' : 'bg-ink-300 dark:bg-ink-700'
        )}
      >
        <span
          className={cn(
            'inline-block transform rounded-full bg-white shadow-sm transition-transform',
            s.thumb,
            checked ? s.shift : 'translate-x-1'
          )}
        />
      </button>
      {label && (
        <span className="text-sm font-medium text-ink-800 dark:text-ink-200">
          {label}
        </span>
      )}
    </label>
  );
};

export default Switch;
