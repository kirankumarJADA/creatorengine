import { forwardRef, useId } from 'react';
import { cn } from '../../utils/helpers.js';

/**
 * Labelled text input designed for react-hook-form.
 *
 *   <FormField
 *     label="Email"
 *     type="email"
 *     leftIcon={Mail}
 *     error={errors.email?.message}
 *     {...register('email', EMAIL_RULES)}
 *   />
 *
 * Use {@link PasswordField} for password inputs (adds show/hide toggle).
 */
const FormField = forwardRef(
  (
    {
      label,
      hint,
      error,
      type = 'text',
      leftIcon: LeftIcon,
      rightSlot,
      className,
      id,
      ...rest
    },
    ref
  ) => {
    const generatedId = useId();
    const inputId = id || generatedId;

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={inputId} className="label">
            {label}
          </label>
        )}
        <div className="relative">
          {LeftIcon && (
            <LeftIcon
              size={16}
              className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-ink-400"
            />
          )}
          <input
            ref={ref}
            id={inputId}
            type={type}
            className={cn(
              'input',
              LeftIcon && 'pl-10',
              rightSlot && 'pr-10',
              error && 'input-error',
              className
            )}
            aria-invalid={!!error}
            aria-describedby={error ? `${inputId}-error` : undefined}
            {...rest}
          />
          {rightSlot && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2">
              {rightSlot}
            </div>
          )}
        </div>
        {error ? (
          <p id={`${inputId}-error`} className="error-text">
            {error}
          </p>
        ) : hint ? (
          <p className="helper-text">{hint}</p>
        ) : null}
      </div>
    );
  }
);

FormField.displayName = 'FormField';
export default FormField;
