/**
 * Tiny helpers shared across components.
 */

/** Conditionally concatenate class names, skipping falsy values. */
export const cn = (...classes) => classes.filter(Boolean).join(' ');

/** Generate initials from a full name (e.g. "Jane Doe" → "JD"). */
export const getInitials = (name = '') => {
  return name
    .split(' ')
    .map((part) => part.charAt(0))
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase();
};

/**
 * Render an ISO date string as a short locale date — e.g. "May 22, 2026".
 * Returns an empty string when the input is falsy or unparseable so
 * table cells stay tidy.
 */
export const formatDate = (input) => {
  if (!input) return '';
  const d = input instanceof Date ? input : new Date(input);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString(undefined, {
    year:  'numeric',
    month: 'short',
    day:   'numeric',
  });
};

/**
 * Render an ISO date string as a relative time — "3m ago", "2h ago",
 * "Yesterday", or falls back to {@link formatDate} for older dates.
 * Designed for "last interaction" / "connected at" UI labels where
 * exact dates matter less than freshness.
 */
export const formatRelative = (input) => {
  if (!input) return '';
  const d = input instanceof Date ? input : new Date(input);
  if (Number.isNaN(d.getTime())) return '';

  const diffMs = Date.now() - d.getTime();
  const diffSec = Math.round(diffMs / 1000);
  if (diffSec < 0)   return formatDate(d);   // future
  if (diffSec < 60)  return 'just now';

  const diffMin = Math.round(diffSec / 60);
  if (diffMin < 60)  return `${diffMin}m ago`;

  const diffHr = Math.round(diffMin / 60);
  if (diffHr < 24)   return `${diffHr}h ago`;

  const diffDay = Math.round(diffHr / 24);
  if (diffDay === 1) return 'yesterday';
  if (diffDay < 7)   return `${diffDay}d ago`;

  return formatDate(d);
};
