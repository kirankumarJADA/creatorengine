import { forwardRef, useId } from 'react';
import { cn } from '../../utils/helpers.js';

const Checkbox = forwardRef(({ label, className, id, ...rest }, ref) => {
  const generatedId = useId();
  const inputId = id || generatedId;

  return (
    <label
      htmlFor={inputId}
      className={cn('inline-flex items-center gap-2 text-sm text-ink-700', className)}
    >
      <input
        ref={ref}
        id={inputId}
        type="checkbox"
        className="h-4 w-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500"
        {...rest}
      />
      {label}
    </label>
  );
});

Checkbox.displayName = 'Checkbox';
export default Checkbox;
