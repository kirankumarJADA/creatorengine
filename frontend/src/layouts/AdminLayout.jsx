import { Outlet, NavLink, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  LayoutDashboard, Users, Workflow, ScrollText,
  AlertOctagon, Activity, ArrowLeft, ShieldCheck,
} from 'lucide-react';
import { ROUTES, APP_NAME } from '../utils/constants.js';
import { cn } from '../utils/helpers.js';

const NAV = [
  { to: ROUTES.ADMIN,             label: 'Dashboard',    icon: LayoutDashboard, end: true },
  { to: ROUTES.ADMIN_USERS,       label: 'Users',        icon: Users },
  { to: ROUTES.ADMIN_AUTOMATIONS, label: 'Automations',  icon: Workflow },
  { to: ROUTES.ADMIN_LOGS,        label: 'Activity Logs', icon: ScrollText },
  { to: ROUTES.ADMIN_FAILED_JOBS, label: 'Failed Jobs',  icon: AlertOctagon },
  { to: ROUTES.ADMIN_SYSTEM,      label: 'System Status', icon: Activity },
];

const AdminLayout = () => (
  <div className="flex min-h-screen bg-ink-50 dark:bg-ink-950">
    <aside className="hidden w-64 shrink-0 flex-col border-r border-ink-800 bg-ink-950 lg:flex">
      <div className="flex items-center gap-2.5 border-b border-ink-800 px-5 py-5">
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 shadow-elevated">
          <ShieldCheck size={18} className="text-white" />
        </span>
        <div>
          <p className="text-sm font-semibold text-white">{APP_NAME}</p>
          <p className="text-[11px] uppercase tracking-wider text-amber-400">Admin</p>
        </div>
      </div>

      <nav className="flex-1 space-y-0.5 px-3 py-4">
        {NAV.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-amber-500/15 text-amber-300'
                  : 'text-ink-400 hover:bg-ink-800 hover:text-ink-100'
              )
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-ink-800 p-3">
        <Link
          to={ROUTES.DASHBOARD}
          className="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-medium text-ink-400 transition-colors hover:bg-ink-800 hover:text-ink-100"
        >
          <ArrowLeft size={16} />
          Back to app
        </Link>
      </div>
    </aside>

    <div className="flex min-w-0 flex-1 flex-col">
      <header className="flex h-14 items-center justify-between border-b border-amber-500/20 bg-amber-500/5 px-6">
        <p className="text-xs font-medium uppercase tracking-wider text-amber-600 dark:text-amber-400">
          Admin mode — visible to ADMIN accounts only
        </p>
      </header>

      <main className="flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
        <motion.div
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2 }}
          className="mx-auto w-full max-w-7xl"
        >
          <Outlet />
        </motion.div>
      </main>
    </div>
  </div>
);

export default AdminLayout;