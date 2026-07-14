import { Outlet, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';

import Sidebar from '../components/Sidebar.jsx';
import Topbar from '../components/Topbar.jsx';
import ErrorBoundary from '../components/ErrorBoundary.jsx';
import { useUiStore } from '../store/uiStore.js';

const AppLayout = () => {
  const isSidebarOpen    = useUiStore((s) => s.isSidebarOpen);
  const isMobileNavOpen  = useUiStore((s) => s.isMobileNavOpen);
  const setMobileNavOpen = useUiStore((s) => s.setMobileNavOpen);
  const location         = useLocation();

  return (
    <div className="flex min-h-screen bg-ink-50 dark:bg-ink-950">
      {/* Desktop sidebar */}
      <aside
        className={`hidden shrink-0 border-r border-ink-100 bg-white transition-all duration-300 dark:border-ink-800 dark:bg-ink-900 lg:flex ${
          isSidebarOpen ? 'w-64' : 'w-20'
        }`}
      >
        <Sidebar collapsed={!isSidebarOpen} />
      </aside>

      {/* Mobile drawer (unchanged) */}
      <AnimatePresence>
        {isMobileNavOpen && (
          <>
            <motion.div
              key="backdrop"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setMobileNavOpen(false)}
              className="fixed inset-0 z-40 bg-black/40 backdrop-blur-sm lg:hidden"
              aria-hidden="true"
            />
            <motion.aside
              key="drawer"
              initial={{ x: -300 }}
              animate={{ x: 0 }}
              exit={{ x: -300 }}
              transition={{ type: 'spring', stiffness: 320, damping: 30 }}
              className="fixed inset-y-0 left-0 z-50 w-72 border-r border-ink-100 bg-white shadow-elevated dark:border-ink-800 dark:bg-ink-900 lg:hidden"
            >
              <Sidebar onNavigate={() => setMobileNavOpen(false)} />
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* Main column — keeps the fade-IN, no "wait for exit" (that caused the blank) */}
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="relative flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
          {/*
            Subtle radial gradient — adds depth without hurting readability.
            Opacity is intentionally very low; it should be barely perceptible.
            pointer-events-none keeps it out of the interaction layer.
          */}
          <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-x-0 top-0 h-80 opacity-[0.4] dark:opacity-[0.18]"
            style={{
              background:
                'radial-gradient(ellipse 75% 100% at 50% -5%, rgba(99,102,241,0.10) 0%, transparent 70%)',
            }}
          />
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.2 }}
            className="mx-auto w-full max-w-7xl"
          >
            <ErrorBoundary>
              <Outlet />
            </ErrorBoundary>
          </motion.div>
        </main>
      </div>
    </div>
  );
};

export default AppLayout;