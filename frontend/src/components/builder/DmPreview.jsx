import { Camera, Phone, Video } from 'lucide-react';
import { motion } from 'framer-motion';
import { cn, getInitials } from '../../utils/helpers.js';

/**
 * A faux Instagram DM chat showing how the rendered message will
 * appear to the recipient. Pure presentational — does no template
 * rendering itself; pass `message` already substituted.
 */
const DmPreview = ({
  message = '',
  recipientHandle = 'follower',
  recipientName,
  brandHandle = 'you',
  link = null,
  className,
}) => {
  const displayName = recipientName || recipientHandle;

  return (
    <div
      className={cn(
        'mx-auto w-full max-w-xs overflow-hidden rounded-[28px] border border-ink-200 bg-white shadow-elevated',
        'dark:border-ink-800 dark:bg-ink-950',
        className
      )}
    >
      {/* Phone status bar */}
      <div className="flex items-center justify-between px-5 pt-2 text-[10px] font-semibold text-ink-700 dark:text-ink-300">
        <span>9:41</span>
        <span className="flex items-center gap-1">
          <span className="h-1.5 w-1.5 rounded-full bg-ink-700 dark:bg-ink-300" />
          <span className="h-1.5 w-1.5 rounded-full bg-ink-700 dark:bg-ink-300" />
          <span className="h-1.5 w-1.5 rounded-full bg-ink-700 dark:bg-ink-300" />
        </span>
      </div>

      {/* Header */}
      <div className="flex items-center justify-between border-b border-ink-100 px-4 py-3 dark:border-ink-800">
        <div className="flex items-center gap-2">
          <span className="grid h-8 w-8 place-items-center rounded-full bg-gradient-to-br from-pink-500 via-fuchsia-500 to-amber-400 text-[10px] font-semibold text-white">
            {getInitials(displayName).slice(0, 2) || 'IG'}
          </span>
          <div className="leading-tight">
            <p className="text-xs font-semibold text-ink-900 dark:text-ink-100">
              @{recipientHandle}
            </p>
            <p className="text-[10px] text-ink-500 dark:text-ink-400">
              Active now
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2 text-ink-500 dark:text-ink-400">
          <Phone size={14} />
          <Video size={14} />
        </div>
      </div>

      {/* Bubble */}
      <div className="space-y-2 bg-ink-50/40 p-4 dark:bg-ink-900/60 min-h-[140px]">
        <p className="text-center text-[10px] text-ink-400 dark:text-ink-500">
          Today · 9:41 AM
        </p>
        <motion.div
          key={message + (link || '')}
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2 }}
          className="flex justify-start"
        >
          <div className="max-w-[80%] rounded-2xl rounded-bl-sm bg-white px-3 py-2 text-xs leading-snug text-ink-900 shadow-sm dark:bg-ink-800 dark:text-ink-100">
            {message ? (
              <p className="whitespace-pre-wrap break-words">{message}</p>
            ) : (
              <p className="italic text-ink-400 dark:text-ink-500">
                Your message preview will appear here…
              </p>
            )}
            {link && (
              <p className="mt-1 break-all text-[11px] text-brand-700 underline dark:text-brand-300">
                {link}
              </p>
            )}
            <p className="mt-1 text-[9px] uppercase tracking-wider text-ink-400 dark:text-ink-500">
              from @{brandHandle}
            </p>
          </div>
        </motion.div>
      </div>

      {/* Footer composer (decorative) */}
      <div className="flex items-center gap-2 border-t border-ink-100 px-3 py-2 dark:border-ink-800">
        <Camera size={14} className="text-brand-600" />
        <div className="h-7 flex-1 rounded-full border border-ink-200 dark:border-ink-800" />
      </div>
    </div>
  );
};

export default DmPreview;
