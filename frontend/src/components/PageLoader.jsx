import { Loader2 } from 'lucide-react';

const PageLoader = () => (
  <div className="grid min-h-screen place-items-center bg-ink-50">
    <div className="flex flex-col items-center gap-3 text-ink-500">
      <Loader2 size={24} className="animate-spin" />
      <p className="text-sm">Loading…</p>
    </div>
  </div>
);

export default PageLoader;
