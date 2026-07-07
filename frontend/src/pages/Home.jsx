import { Suspense, lazy } from 'react';
import { Link } from 'react-router-dom';
import { ROUTES } from '../utils/constants.js';

const EngineScene = lazy(() => import('../three/EngineScene.jsx'));

const Fallback = () => (
  <div className="grid h-screen w-full place-items-center bg-[#050508]">
    <div className="text-center">
      <img src="/logo-mark.png" alt="" className="mx-auto h-12 w-12 animate-pulse" />
      <p className="mt-4 font-mono text-xs uppercase tracking-[0.3em] text-ink-500">
        Initializing engine…
      </p>
    </div>
  </div>
);

const Home = () => (
  <div className="h-screen w-full overflow-hidden bg-[#050508]">
    {/* Fixed top nav over the 3D canvas */}
    <header className="pointer-events-none fixed inset-x-0 top-0 z-50 flex items-center justify-between px-6 py-5 sm:px-10">
      <div className="pointer-events-auto flex items-center gap-2.5">
        <img src="/logo-mark.png" alt="CreatorEngine" className="h-9 w-9" />
        <span className="text-lg font-semibold tracking-tight text-white">CreatorEngine</span>
      </div>
      <div className="pointer-events-auto flex items-center gap-3">
        <Link
          to={ROUTES.LOGIN}
          className="rounded-xl px-4 py-2 text-sm font-medium text-ink-300 transition-colors hover:text-white"
        >
          Sign in
        </Link>
        <Link
          to={ROUTES.REGISTER}
          className="rounded-xl bg-white px-4 py-2 text-sm font-semibold text-ink-950 shadow-elevated transition-transform hover:-translate-y-px"
        >
          Get started
        </Link>
      </div>
    </header>

    <Suspense fallback={<Fallback />}>
      <EngineScene />
    </Suspense>
  </div>
);

export default Home;