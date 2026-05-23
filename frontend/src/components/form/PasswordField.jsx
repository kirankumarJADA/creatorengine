import { forwardRef, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import FormField from './FormField.jsx';

/**
 * Password input with a show/hide toggle.
 * Forwards refs so it works seamlessly with react-hook-form's `register`.
 */
const PasswordField = forwardRef((props, ref) => {
  const [visible, setVisible] = useState(false);

  const toggle = (
    <button
      type="button"
      onClick={() => setVisible((v) => !v)}
      className="text-ink-400 transition-colors hover:text-ink-700"
      aria-label={visible ? 'Hide password' : 'Show password'}
      tabIndex={-1}
    >
      {visible ? <EyeOff size={16} /> : <Eye size={16} />}
    </button>
  );

  return (
    <FormField
      ref={ref}
      type={visible ? 'text' : 'password'}
      rightSlot={toggle}
      {...props}
    />
  );
});

PasswordField.displayName = 'PasswordField';
export default PasswordField;
