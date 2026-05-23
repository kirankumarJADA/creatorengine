/**
 * Validation rules used by react-hook-form's `register()`.
 *
 * Centralising them keeps the Login/Register/ForgotPassword forms in
 * lockstep with each other and with the backend's bean-validation rules.
 */

export const EMAIL_RULES = {
  required: 'Email is required',
  pattern: {
    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
    message: 'Enter a valid email address',
  },
  maxLength: { value: 254, message: 'Email is too long' },
};

export const PASSWORD_RULES = {
  required: 'Password is required',
  minLength: { value: 8, message: 'Password must be at least 8 characters' },
  maxLength: { value: 128, message: 'Password is too long' },
  validate: {
    hasLetter: (v) => /[A-Za-z]/.test(v) || 'Must contain at least one letter',
    hasNumber: (v) => /\d/.test(v) || 'Must contain at least one number',
  },
};

export const LOGIN_PASSWORD_RULES = {
  // Looser rules on login — we just need *something* to send.
  required: 'Password is required',
};

export const NAME_RULES = {
  required: 'Name is required',
  minLength: { value: 2, message: 'Name must be at least 2 characters' },
  maxLength: { value: 80, message: 'Name is too long' },
};
