import { useState, useEffect, useRef } from 'react';
import { NavLink, Link } from 'react-router-dom';
import {
  LayoutDashboard,
  Workflow,
  Users,
  ScrollText,
  AlertOctagon,
  Settings as SettingsIcon,
  LogOut,
  ChevronLeft,
  Instagram,
  ShieldCheck,
  ChevronDown,
  Plus,
  Check,
} from 'lucide-react';

import { useAuthStore } from '../store/authStore.js';
import { useUiStore } from '../store/uiStore.js';
import { useAccountStore } from '../store/accountStore.js';
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

  const accounts = useAccountStore((s) => s.accounts);
  const activeAccount = useAccountStore((s) => s.activeAccount);
  const fetchAccounts = useAccountStore((s) => s.fetchAccounts);
  const setActiveAccount = useAccountStore((s) => s.setActiveAccount);
  const isLoading = useAccountStore((s) => s.isLoading);

  const [switcherOpen, setSwitcherOpen] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const switcherRef = useRef(null);

  const isAdmin = Array.isArray(user?.roles) && user.roles.includes('ADMIN');

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  // Close switcher on outside click
  useEffect(() => {
    const handler = (e) => {
      if (switcherRef.current && !switcherRef.current.contains(e.target)) {
        setSwitcherOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleConnect = async () => {
    if (connecting) return;
    setConnecting(true);
    setSwitcherOpen(false);
    try {
      const data = await instagramService.startConnect();
      const url = data?.authUrl || data?.url;
      if (url) {
        window.location.href = url;
        return;
      }
    } catch {
      /* fall through */
    }
    setConnecting(false);
  };

  const handleSelectAccount = (account) => {
    setActiveAccount(account);
    setSwitcherOpen(false);
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
          <img
            src="/logo-mark.png"
            alt="CreatorEngine"
            className="h-9 w-9 shrink-0 object-contain"
          />
          {!collapsed && (
            <span className="text-lg font-semibold tracking-tight text-ink-900 dark:text-ink-100">
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

      {/* Account switcher */}
      {!collapsed && (
        <div className="px-4 pt-4" ref={switcherRef}>
          {isLoading && accounts.length === 0 ? (
            /* Loading skeleton */
            <div className="flex w-full items-center gap-2.5 rounded-xl border border-ink-200 bg-ink-50/40 px-3 py-2.5 dark:border-ink-800 dark:bg-ink-800/30">
              <span className="h-7 w-7 shrink-0 animate-pulse rounded-lg bg-ink-200 dark:bg-ink-700" />
              <span className="text-xs text-ink-400 dark:text-ink-500">Loading accounts…</span>
            </div>
          ) : accounts.length === 0 ? (
            /* No accounts — connect CTA */
            <button
              type="button"
              onClick={handleConnect}
              disabled={connecting}
              className="group flex w-full items-center justify-between rounded-xl border border-dashed border-ink-200 bg-ink-50/40 px-3 py-2.5 text-left text-sm transition-all hover:-translate-y-px hover:border-brand-300 hover:bg-brand-50/30 hover:shadow-soft disabled:opacity-60 dark:border-ink-800 dark:bg-ink-800/30 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/5"
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
              <span className="text-xs font-medium text-ink-400 transition-transform group-hover:translate-x-0.5 group-hover:text-brand-600 dark:group-hover:text-brand-400">
                →
              </span>
            </button>
          ) : (
            /* Account switcher */
            <div className="relative">
              <button
                type="button"
                onClick={() => setSwitcherOpen((v) => !v)}
                className="group flex w-full items-center justify-between rounded-xl border border-ink-200 bg-ink-50/40 px-3 py-2.5 text-left text-sm transition-all hover:-translate-y-px hover:border-brand-300 hover:bg-brand-50/30 hover:shadow-soft dark:border-ink-800 dark:bg-ink-800/30 dark:hover:border-brand-500/40 dark:hover:bg-brand-500/5"
              >
                <span className="flex min-w-0 items-center gap-2.5">
                  {activeAccount?.profilePictureUrl ? (
                    <img
                      src={activeAccount.profilePictureUrl}
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
                      {activeAccount?.name || activeAccount?.username || 'Select account'}
                    </span>
                    <span className="block truncate text-xs text-ink-500 dark:text-ink-400">
                      {activeAccount?.username ? `@${activeAccount.username}` : 'No account selected'}
                    </span>
                  </span>
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="relative flex h-2 w-2 shrink-0" title="Connected">
                    <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />
                    <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-500" />
                  </span>
                  <ChevronDown
                    size={14}
                    className={cn(
                      'shrink-0 text-ink-400 transition-transform',
                      switcherOpen && 'rotate-180'
                    )}
                  />
                </span>
              </button>

              {/* Dropdown */}
              {switcherOpen && (
                <div className="absolute left-0 right-0 top-full z-50 mt-1.5 rounded-xl border border-ink-100 bg-white shadow-elevated dark:border-ink-800 dark:bg-ink-900">
                  <div className="p-1.5">
                    {accounts.map((account) => {
                      const isActive = activeAccount?.instagramUserId === account.instagramUserId;
                      return (
                        <button
                          key={account.instagramUserId}
                          type="button"
                          onClick={() => handleSelectAccount(account)}
                          className={cn(
                            'flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm transition-colors',
                            isActive
                              ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
                              : 'text-ink-700 hover:bg-ink-50 dark:text-ink-200 dark:hover:bg-ink-800'
                          )}
                        >
                          {account.profilePictureUrl ? (
                            <img
                              src={account.profilePictureUrl}
                              alt=""
                              referrerPolicy="no-referrer"
                              className="h-6 w-6 shrink-0 rounded-md object-cover"
                            />
                          ) : (
                            <span className="grid h-6 w-6 shrink-0 place-items-center rounded-md bg-gradient-to-br from-pink-500 via-fuchsia-500 to-amber-400 text-white">
                              <Instagram size={12} />
                            </span>
                          )}
                          <span className="min-w-0 flex-1">
                            <span className="block truncate font-medium">
                              {account.name || account.username}
                            </span>
                            <span className="block truncate text-xs opacity-60">
                              @{account.username}
                            </span>
                          </span>
                          {isActive && (
                            <Check size={14} className="shrink-0 text-brand-600 dark:text-brand-400" />
                          )}
                        </button>
                      );
                    })}
                  </div>

                  {/* Connect another account */}
                  <div className="border-t border-ink-100 p-1.5 dark:border-ink-800">
                    <button
                      type="button"
                      onClick={handleConnect}
                      disabled={connecting}
                      className="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm text-ink-600 transition-colors hover:bg-ink-50 disabled:opacity-60 dark:text-ink-400 dark:hover:bg-ink-800"
                    >
                      <span className="grid h-6 w-6 shrink-0 place-items-center rounded-md border border-dashed border-ink-300 dark:border-ink-700">
                        <Plus size={12} />
                      </span>
                      <span className="font-medium">
                        {connecting ? 'Connecting…' : 'Connect another account'}
                      </span>
                    </button>
                  </div>
                </div>
              )}
            </div>
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
                        'group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-150',
                        collapsed && 'justify-center px-2',
                        isActive
                          ? 'bg-brand-600 text-white shadow-soft'
                          : 'text-ink-700 hover:translate-x-0.5 hover:bg-ink-100 hover:text-ink-950 dark:text-ink-300 dark:hover:bg-ink-800 dark:hover:text-ink-100'
                      )
                    }
                  >
                    {({ isActive }) => (
                      <>
                        {isActive && !collapsed && (
                          <span className="absolute -left-3 h-5 w-[3px] rounded-r-full bg-brand-400" />
                        )}
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

        {isAdmin && (
          <div className="mb-6 last:mb-0">
            {!collapsed && (
              <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-amber-500">
                Admin
              </p>
            )}
            <ul className="space-y-0.5">
              <li>
                <NavLink
                  to={ROUTES.ADMIN}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                      collapsed && 'justify-center px-2',
                      isActive
                        ? 'bg-amber-500 text-white shadow-soft'
                        : 'text-amber-700 hover:bg-amber-50 dark:text-amber-400 dark:hover:bg-amber-500/10'
                    )
                  }
                >
                  <ShieldCheck size={18} className="shrink-0" />
                  {!collapsed && <span>Admin</span>}
                </NavLink>
              </li>
            </ul>
          </div>
        )}
      </nav>

      {/* User footer */}
      <div className="border-t border-ink-100 p-3 dark:border-ink-800">
        <div
          className={cn(
            'flex items-center gap-3 rounded-xl px-3 py-2 transition-colors hover:bg-ink-50 dark:hover:bg-ink-800/50',
            collapsed && 'justify-center px-2'
          )}
        >
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-gradient-to-br from-brand-100 to-brand-200 text-sm font-semibold text-brand-700 ring-1 ring-brand-200/60 dark:from-brand-500/20 dark:to-brand-500/10 dark:text-brand-300 dark:ring-brand-500/20">
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