import { useState, useEffect } from 'react';
import { NavLink, Link } from 'react-router-dom';
import {
  LayoutDashboard,
  Workflow,
  Users,
  ScrollText,
  AlertOctagon,
  Settings as SettingsIcon,
  Sparkles,
  LogOut,
  ChevronLeft,
  Instagram,
} from 'lucide-react';

import { useAuthStore } from '../store/authStore.js';
import { useUiStore } from '../store/uiStore.js';
import instagramService from '../services/instagramService.js';
import { ROUTES, APP_NAME } from '../utils/constants.js';
import { cn, getInitials } from '../utils/helpers.js';
import IconButton from './ui/IconButton.jsx';

const NAV_GROUPS = [
  {
    label: 'Workspace',
    items: [
      { to: ROUTES.DASHBOARD,   label: 'Dashboard',     icon: LayoutDashboard },
      { to: ROUTES.AUTOMATIONS, label: 'Automations',   icon: Workflow },
      { to: ROUTES.CONTACTS,    label: 'Contacts',      icon: Users },
      { to: ROUTES.LOGS,        label: 'Activity Logs', icon: ScrollText },
      { to: ROUTES.FAILED_JOBS, label: 'Failed Jobs',   icon: AlertOctagon },
    ],
  },
  {
    label: 'Account',
    items: [
      { to: ROUTES.SETTINGS, label: 'Settings', icon: SettingsIcon },
    ],
  },
];

const Sidebar = ({ collapsed = false, onNavigate }) => {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const toggleSidebar = useUiStore((s) => s.toggleSidebar);

  const [igStatus, setIgStatus] = useState(null);   // null = loading
  const [connecting, setConnecting] = useState(false);

  useEffect(() => {
    let active = true;
    instagramService
      .getStatus()
      .then((s) => { if (active) setIgStatus(s || { status: 'NOT_CONNECTED' }); })
      .catch(() => { if (active) setIgStatus({ status: 'NOT_CONNECTED' }); });
    return () => { active = false; };
  }, []);

  const igConnected = Boolean(igStatus?.username);

  const handleConnect = async () => {
    if (connecting) return;
    setConnecting(true);
    try {
      const data = await instagramService.startConnect();
      const url = data?.authUrl || data?.url;
      if (url) {
        window.location.href = url;
        return; // navigating away
      }
    } catch {
      /* fall through to reset */
    }
    setConnecting(false);
  };

  return (
    <div className="flex h-full w-full flex-col">
      {/* Logo + collapse toggle */}
      <div
        className={cn(
          'flex items-center border-b border-ink-100 px-4 py-5 dark:border-ink-800',
          collapsed ? 'justify-center' : 'justify-between'
        )}
      >
        <Link
          to={ROUTES.DASHBOARD}
          onClick={onNavigate}
          className="inline-flex items-center gap-2.5"
        >
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-ink-900 text-white shadow-sm dark:bg-brand-600">
            <Sparkles size={18} strokeWidth={2.5} />
          </span>
          {!collapsed && (
            <span className="text-lg font-semibold text-ink-900 dark:text-ink-100">
              {APP_NAME}
            </span>
          )}
        </Link>

        {!collapsed && (
          <IconButton
            onClick={toggleSidebar}
            aria-label="Collapse sidebar"
            size="sm"
            className="hidden lg:inline-flex"
          >
            <ChevronLeft size={16} />
          </IconButton>
        )}
      </div>

      {/* Instagram card: loading / connected profile / connect CTA */}
      {!collapsed && (
        <div className="px-4 pt-4">
          {igStatus === null ? (
            <div className="flex w-full items-center gap-2.5 rounded-xl border border-ink-200 bg-ink-50/40 px-3 py-2.5 dark:border-ink-800 dark:bg-ink-800/30">
              <span className="h-7 w-7 shrink-0 animate-pulse rounded-lg bg-ink-200 dark:bg-ink-700" />
              <span className="text-xs text-ink-400 dark:text-ink-500">Checking Instagram…</span>
            </div>
          ) : igConnected ? (
            <Link
              to={ROUTES.SETTINGS}
              onClick={onNavigate}
              className="group flex w-full items-center justify-between rounded-xl border border-ink-200 bg-ink-50/40 px-3 py-2.5 text-left text-sm transition-colors hover:border-brand-300 hover:bg-brand-50/30 dark:border-ink-800 dark:bg-ink-800/30 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/5"
            >
              <span className="flex min-w-0 items-center gap-2.5">
                {igStatus.profilePictureUrl ? (
                  <img
                    src={igStatus.profilePictureUrl}
                    alt=""
                    referrerPolicy="no-referrer"
                    className="h-7 w-7 shrink-0 rounded-lg object-cover"
                  />
                ) : (
                  <span className="grid h-7 w-7 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-pink-500 via-fuchsia-500 to-amber-400 text-white">
                    <Instagram size={14} />
                  </span>
                )}
                <span className="min-w-0">
                  <span className="block truncate font-medium text-ink-900 dark:text-ink-100">
                    {igStatus.name || igStatus.username}
                  </span>
                  <span className="block truncate text-xs text-ink-500 dark:text-ink-400">
                    @{igStatus.username}
                  </span>
                </span>
              </span>
              <span className="h-2 w-2 shrink-0 rounded-full bg-emerald-500" title="Connected" />
            </Link>
          ) : (
            <button
              type="button"
              onClick={handleConnect}
              disabled={connecting}
              className="group flex w-full items-center justify-between rounded-xl border border-dashed border-ink-200 bg-ink-50/40 px-3 py-2.5 text-left text-sm transition-colors hover:border-brand-300 hover:bg-brand-50/30 disabled:opacity-60 dark:border-ink-800 dark:bg-ink-800/30 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/5"
            >
              <span className="flex min-w-0 items-center gap-2.5">
                <span className="grid h-7 w-7 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-pink-500 via-fuchsia-500 to-amber-400 text-white">
                  <Instagram size={14} />
                </span>
                <span className="min-w-0">
                  <span className="block truncate font-medium text-ink-900 dark:text-ink-100">
                    {connecting ? 'Connecting…' : 'Connect Instagram'}
                  </span>
                  <span className="block truncate text-xs text-ink-500 dark:text-ink-400">
                    Link your Business account
                  </span>
                </span>
              </span>
              <span className="text-xs font-medium text-ink-400 group-hover:text-brand-600 dark:group-hover:text-brand-400">
                →
              </span>
            </button>
          )}
        </div>
      )}

      {/* Nav groups */}
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        {NAV_GROUPS.map((group) => (
          <div key={group.label} className="mb-6 last:mb-0">
            {!collapsed && (
              <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-ink-400 dark:text-ink-500">
                {group.label}
              </p>
            )}
            <ul className="space-y-0.5">
              {group.items.map(({ to, label, icon: Icon }) => (
                <li key={to}>
                  <NavLink
                    to={to}
                    onClick={onNavigate}
                    end={to === ROUTES.DASHBOARD}
                    className={({ isActive }) =>
                      cn(
                        'group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                        collapsed && 'justify-center px-2',
                        isActive
                          ? 'bg-brand-600 text-white shadow-sm'
                          : 'text-ink-700 hover:bg-ink-100 hover:text-ink-950 dark:text-ink-300 dark:hover:bg-ink-800 dark:hover:text-ink-100'
                      )
                    }
                  >
                    {({ isActive }) => (
                      <>
                        <Icon size={18} strokeWidth={isActive ? 2.25 : 2} className="shrink-0" />
                        {!collapsed && <span>{label}</span>}
                      </>
                    )}
                  </NavLink>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </nav>

      {/* User footer */}
      <div className="border-t border-ink-100 p-3 dark:border-ink-800">
        <div
          className={cn(
            'flex items-center gap-3 rounded-xl px-3 py-2',
            collapsed && 'justify-center px-2'
          )}
        >
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-brand-100 text-sm font-semibold text-brand-700 dark:bg-brand-500/15 dark:text-brand-300">
            {user?.name ? getInitials(user.name) : 'U'}
          </span>
          {!collapsed && (
            <>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-ink-900 dark:text-ink-100">
                  {user?.name || 'Guest'}
                </p>
                <p className="truncate text-xs text-ink-500 dark:text-ink-400">
                  {user?.email || ''}
                </p>
              </div>
              <IconButton
                onClick={logout}
                aria-label="Sign out"
                size="sm"
                tone="danger"
              >
                <LogOut size={14} />
              </IconButton>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default Sidebar;