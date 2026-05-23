import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { X } from 'lucide-react';
import IconButton from './IconButton.jsx';
import { cn } from '../../utils/helpers.js';

/**
 * Centred modal with backdrop, ESC-to-close, body-scroll lock,
 * and a portal mount.
 *
 * Intentionally tiny: there's no focus trap, no return-focus. For a
 * real product we'd reach for @headlessui/react or Radix Dialog;
 * this is fine for the wizard's simulator and any other dev-grade
 * modal we open from the dashboard.
 */
const Modal = ({
  open,
  onClose,
  title,
  description,
  size = 'md',
  children,
  footer,
}) => {
  // Body-scroll lock + ESC handler
  useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const onKey = (e) => e.key === 'Escape' && onClose?.();
    document.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = prev;
      document.removeEventListener('keydown', onKey);
    };
  }, [open, onClose]);

  if (typeof document === 'undefined') return null;

  const widths = {
    sm: 'max-w-sm',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-3xl',
  };

  return createPortal(
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50 flex items-end justify-center p-4 sm:items-center">
          <motion.div
            key="backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="absolute inset-0 bg-black/50 backdrop-blur-sm"
            aria-hidden="true"
          />
          <motion.div
            key="panel"
            initial={{ opacity: 0, y: 20, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.98 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            role="dialog"
            aria-modal="true"
            aria-labelledby={title ? 'modal-title' : undefined}
            className={cn(
              'relative z-10 w-full overflow-hidden rounded-2xl border shadow-elevated',
              'border-ink-100 bg-white dark:border-ink-800 dark:bg-ink-900',
              widths[size]
            )}
          >
            {(title || onClose) && (
              <div className="flex items-start justify-between gap-4 border-b border-ink-100 px-5 py-4 dark:border-ink-800">
                <div>
                  {title && (
                    <h2
                      id="modal-title"
                      className="text-base font-semibold text-ink-900 dark:text-ink-100"
                    >
                      {title}
                    </h2>
                  )}
                  {description && (
                    <p className="mt-0.5 text-sm text-ink-500 dark:text-ink-400">
                      {description}
                    </p>
                  )}
                </div>
                {onClose && (
                  <IconButton onClick={onClose} aria-label="Close">
                    <X size={16} />
                  </IconButton>
                )}
              </div>
            )}
            <div className="px-5 py-5">{children}</div>
            {footer && (
              <div className="flex items-center justify-end gap-2 border-t border-ink-100 bg-ink-50/50 px-5 py-3 dark:border-ink-800 dark:bg-ink-900/40">
                {footer}
              </div>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    document.body
  );
};

export default Modal;
