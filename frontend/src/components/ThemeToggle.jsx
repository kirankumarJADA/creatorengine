import { Moon, Sun } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import IconButton from './ui/IconButton.jsx';
import { useUiStore } from '../store/uiStore.js';

/**
 * Theme toggle button — flips the {@code dark} class on the html
 * element via the UI store and persists the preference.
 *
 * The icon swap is animated with Framer Motion's AnimatePresence so
 * we don't get a hard cut between sun and moon on every click.
 */
const ThemeToggle = ({ className }) => {
  const theme = useUiStore((s) => s.theme);
  const toggleTheme = useUiStore((s) => s.toggleTheme);
  const isDark = theme === 'dark';

  return (
    <IconButton
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light theme' : 'Switch to dark theme'}
      className={className}
    >
      <AnimatePresence mode="wait" initial={false}>
        <motion.span
          key={isDark ? 'moon' : 'sun'}
          initial={{ opacity: 0, rotate: -90, scale: 0.8 }}
          animate={{ opacity: 1, rotate: 0, scale: 1 }}
          exit={{ opacity: 0, rotate: 90, scale: 0.8 }}
          transition={{ duration: 0.15 }}
          className="grid place-items-center"
        >
          {isDark ? <Moon size={16} /> : <Sun size={16} />}
        </motion.span>
      </AnimatePresence>
    </IconButton>
  );
};

export default ThemeToggle;
