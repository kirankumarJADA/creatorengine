import { Moon, Sun, Monitor } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import IconButton from './ui/IconButton.jsx';
import { useUiStore } from '../store/uiStore.js';

const NEXT_LABEL = {
  light:  'Switch to dark theme',
  dark:   'Switch to system theme',
  system: 'Switch to light theme',
};

const ThemeIcon = ({ theme }) => {
  if (theme === 'dark')   return <Moon size={16} />;
  if (theme === 'system') return <Monitor size={16} />;
  return <Sun size={16} />;
};

const ThemeToggle = ({ className }) => {
  const theme = useUiStore((s) => s.theme);
  const toggleTheme = useUiStore((s) => s.toggleTheme);

  return (
    <IconButton
      onClick={toggleTheme}
      aria-label={NEXT_LABEL[theme] || 'Toggle theme'}
      title={NEXT_LABEL[theme]}
      className={className}
    >
      <AnimatePresence mode="wait" initial={false}>
        <motion.span
          key={theme}
          initial={{ opacity: 0, rotate: -90, scale: 0.8 }}
          animate={{ opacity: 1, rotate: 0, scale: 1 }}
          exit={{ opacity: 0, rotate: 90, scale: 0.8 }}
          transition={{ duration: 0.15 }}
          className="grid place-items-center"
        >
          <ThemeIcon theme={theme} />
        </motion.span>
      </AnimatePresence>
    </IconButton>
  );
};

export default ThemeToggle;
