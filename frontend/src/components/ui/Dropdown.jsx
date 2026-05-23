import { useEffect, useRef, useState } from 'react';
import { ChevronDown, Check } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { cn } from '../../utils/helpers.js';

const Dropdown = ({
  options = [], value, onChange, label,
  className, align = 'left',
}) => {
  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    const onClick = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    const onKey = (e) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', onClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const normalised = options.map((o) =>
    typeof o === 'string' ? { label: o, value: o } : o
  );
  const current = normalised.find((o) => o.value === value);

  return (
    <div ref={containerRef} className={cn('relative inline-block', className)}>
      <button
        type="button"
        onClick={() => setOpen((s) => !s)}
        className={cn(
          'inline-flex w-full items-center justify-between gap-2 rounded-xl border',
          'border-ink-200 bg-white px-3.5 py-2.5 text-sm font-medium text-ink-800',
          'transition-colors hover:bg-ink-50',
          'dark:border-ink-800 dark:bg-ink-900 dark:text-ink-200 dark:hover:bg-ink-800/60'
        )}
      >
        <span>
          {label && (
            <span className="mr-1 text-ink-500 dark:text-ink-400">{label}:</span>
          )}
          {current ? current.label : 'Select…'}
        </span>
        <ChevronDown
          size={16}
          className={cn('text-ink-400 transition-transform', open && 'rotate-180')}
        />
      </button>

      <AnimatePresence>
        {open && (
          <motion.ul
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            transition={{ duration: 0.12 }}
            className={cn(
              'absolute z-30 mt-2 min-w-[180px] overflow-hidden rounded-xl border bg-white p-1 shadow-elevated',
              'border-ink-100 dark:border-ink-800 dark:bg-ink-900',
              align === 'right' ? 'right-0' : 'left-0'
            )}
            role="listbox"
          >
            {normalised.map((o) => {
              const active = o.value === value;
              return (
                <li key={o.value}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={active}
                    onClick={() => { onChange?.(o.value); setOpen(false); }}
                    className={cn(
                      'flex w-full items-center justify-between gap-3 rounded-lg px-3 py-2 text-left text-sm',
                      'transition-colors',
                      active
                        ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
                        : 'text-ink-800 hover:bg-ink-100 dark:text-ink-200 dark:hover:bg-ink-800/60'
                    )}
                  >
                    <span>{o.label}</span>
                    {active && <Check size={14} />}
                  </button>
                </li>
              );
            })}
          </motion.ul>
        )}
      </AnimatePresence>
    </div>
  );
};

export default Dropdown;
