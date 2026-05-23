import { forwardRef, useId } from 'react';
import { cn } from '../../utils/helpers.js';

/**
 * Labelled multi-line text input.
 * Pairs cleanly with react-hook-form via {...register('field')}.
 *
 * Pass `showCount` with `maxLength` to render a "120/2000" counter.
 */
const TextArea = forwardRef(
  (
    {
      label,
      hint,
      error,
      maxLength,
      showCount = false,
      value,
      className,
      id,
      rows = 4,
      ...rest
    },
    ref
  ) => {
    const generatedId = useId();
    const inputId = id || generatedId;
    const len = typeof value === 'string' ? value.length : null;

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={inputId} className="label">
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={inputId}
          rows={rows}
          maxLength={maxLength}
          value={value}
          className={cn(
            'input min-h-[5rem] resize-y leading-relaxed',
            error && 'input-error',
            className
          )}
          aria-invalid={!!error}
          aria-describedby={error ? `${inputId}-error` : undefined}
          {...rest}
        />
        <div className="mt-1 flex items-start justify-between gap-3">
          <div className="flex-1">
            {error ? (
              <p id={`${inputId}-error`} className="error-text">{error}</p>
            ) : hint ? (
              <p className="helper-text">{hint}</p>
            ) : null}
          </div>
          {showCount && maxLength != null && len != null && (
            <p className="shrink-0 text-xs text-ink-400 dark:text-ink-500">
              {len}/{maxLength}
            </p>
          )}
        </div>
      </div>
    );
  }
);

TextArea.displayName = 'TextArea';
export default TextArea;
