import { Menu, Search, PanelLeft } from 'lucide-react';
import IconButton from './ui/IconButton.jsx';
import ThemeToggle from './ThemeToggle.jsx';
import { useUiStore } from '../store/uiStore.js';

/**
 * Top navbar that floats above the page content.
 * Sticky + backdrop-blur so it stays legible against any background.
 */
const Topbar = () => {
  const toggleMobileNav  = useUiStore((s) => s.toggleMobileNav);
  const toggleSidebar    = useUiStore((s) => s.toggleSidebar);
  const isSidebarOpen    = useUiStore((s) => s.isSidebarOpen);

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-ink-100 bg-white/80 px-4 backdrop-blur-md dark:border-ink-800 dark:bg-ink-950/80 sm:px-6 lg:px-10">
      {/* Mobile drawer trigger */}
      <IconButton
        onClick={toggleMobileNav}
        aria-label="Open menu"
        className="lg:hidden"
      >
        <Menu size={18} />
      </IconButton>

      {/* Desktop: re-expand sidebar when collapsed */}
      {!isSidebarOpen && (
        <IconButton
          onClick={toggleSidebar}
          aria-label="Expand sidebar"
          className="hidden lg:inline-flex"
        >
          <PanelLeft size={18} />
        </IconButton>
      )}

      {/* Search */}
      <div className="group relative max-w-md flex-1">
        <Search
          size={16}
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 transition-colors group-focus-within:text-brand-500"
        />
        <input
          type="search"
          placeholder="Search automations, contacts…"
          className="input pl-9 pr-14 transition-shadow focus:shadow-soft"
        />
        <kbd className="pointer-events-none absolute right-3 top-1/2 hidden -translate-y-1/2 rounded-md border border-ink-200 bg-ink-50 px-1.5 py-0.5 font-mono text-[10px] font-medium text-ink-400 dark:border-ink-700 dark:bg-ink-800 dark:text-ink-500 sm:block">
          ⌘K
        </kbd>
      </div>

      <div className="ml-auto flex items-center gap-1.5">
        <ThemeToggle />
      </div>
    </header>
  );
};

export default Topbar;