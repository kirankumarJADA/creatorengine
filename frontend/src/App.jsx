import { useEffect } from 'react';
import AppRoutes from './routes/AppRoutes.jsx';
import { useAuthStore } from './store/authStore.js';
import { useUiStore } from './store/uiStore.js';

function App() {
  const bootstrap = useAuthStore((s) => s.bootstrap);
  const initThemeListener = useUiStore((s) => s.initThemeListener);

  useEffect(() => {
    bootstrap();
  }, [bootstrap]);

  useEffect(() => {
    // Keep 'system' theme in sync when OS dark/light mode changes
    const cleanup = initThemeListener();
    return cleanup;
  }, [initThemeListener]);

  return <AppRoutes />;
}

export default App;
