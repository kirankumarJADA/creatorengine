import { useEffect } from 'react';
import AppRoutes from './routes/AppRoutes.jsx';
import { useAuthStore } from './store/authStore.js';

function App() {
  const bootstrap = useAuthStore((s) => s.bootstrap);

  useEffect(() => {
    // Hydrate auth from localStorage once on app start
    bootstrap();
  }, [bootstrap]);

  return <AppRoutes />;
}

export default App;
