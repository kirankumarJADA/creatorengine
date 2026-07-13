import { useEffect, useMemo, useState } from 'react';
import { Search, Users as UsersIcon, Trash2, Instagram } from 'lucide-react';
import toast from 'react-hot-toast';

import PageHeader from '../../components/ui/PageHeader.jsx';
import { Card } from '../../components/ui/Card.jsx';
import Badge from '../../components/ui/Badge.jsx';
import Switch from '../../components/ui/Switch.jsx';
import IconButton from '../../components/ui/IconButton.jsx';
import Modal from '../../components/ui/Modal.jsx';
import Button from '../../components/form/Button.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import adminService from '../../services/adminService.js';
import { formatDate, getInitials } from '../../utils/helpers.js';

const AdminUsers = () => {
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [deleting, setDeleting] = useState(null);

  const load = async () => {
    setIsLoading(true);
    try {
      const data = await adminService.listUsers();
      setUsers(data);
    } catch {
      /* interceptor toasted */
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return users;
    return users.filter((u) =>
      (u.email || '').toLowerCase().includes(q) ||
      (u.name || '').toLowerCase().includes(q)
    );
  }, [users, query]);

  const handleToggle = async (u) => {
    const optimistic = users.map((x) => x.uid === u.uid ? { ...x, enabled: !x.enabled } : x);
    setUsers(optimistic);
    try {
      if (u.enabled) {
        await adminService.disableUser(u.uid);
        toast.success(`${u.email} disabled.`);
      } else {
        await adminService.enableUser(u.uid);
        toast.success(`${u.email} enabled.`);
      }
    } catch {
      setUsers(users); // revert
    }
  };

  const confirmDelete = async () => {
    if (!deleting) return;
    try {
      await adminService.deleteUser(deleting.uid);
      setUsers((s) => s.filter((u) => u.uid !== deleting.uid));
      toast.success('User deleted.');
    } catch {
      /* interceptor toasted */
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div>
      <PageHeader title="Users" description="Every account on the platform." />

      <div className="relative mb-6 max-w-md">
        <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by email or name…"
          className="input pl-9"
        />
      </div>

      <Card padded={false}>
        {isLoading ? (
          <div className="space-y-3 p-5">
            {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
          </div>
        ) : filtered.length === 0 ? (
          <div className="p-8">
            <EmptyState icon={UsersIcon} title="No users found" description="Try a different search." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-ink-100 text-left text-xs uppercase tracking-wider text-ink-400 dark:border-ink-800">
                  <th className="px-5 py-3 font-medium">User</th>
                  <th className="px-5 py-3 font-medium">Instagram</th>
                  <th className="px-5 py-3 font-medium">Signed up</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100 dark:divide-ink-800">
                {filtered.map((u) => (
                  <tr key={u.uid} className="row-hover">
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-brand-100 text-xs font-semibold text-brand-700 dark:bg-brand-500/15 dark:text-brand-300">
                          {getInitials(u.name || u.email)}
                        </span>
                        <div className="min-w-0">
                          <p className="truncate font-medium text-ink-900 dark:text-ink-100">{u.name || '—'}</p>
                          <p className="truncate text-xs text-ink-500 dark:text-ink-400">{u.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      {u.instagramConnected ? (
                        <span className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-700 dark:text-emerald-400">
                          <Instagram size={13} /> @{u.instagramUsername}
                        </span>
                      ) : (
                        <span className="text-xs text-ink-400">Not connected</span>
                      )}
                    </td>
                    <td className="px-5 py-3 text-ink-600 dark:text-ink-300">{formatDate(u.createdAt)}</td>
                    <td className="px-5 py-3">
                      <Badge tone={u.enabled ? 'success' : 'danger'} dot>
                        {u.enabled ? 'Active' : 'Disabled'}
                      </Badge>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <Switch checked={u.enabled} onChange={() => handleToggle(u)} size="sm" />
                        <IconButton tone="danger" size="sm" aria-label="Delete user" onClick={() => setDeleting(u)}>
                          <Trash2 size={14} />
                        </IconButton>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal
        open={Boolean(deleting)}
        onClose={() => setDeleting(null)}
        title="Delete user?"
        description={deleting ? `${deleting.email} will be permanently removed.` : ''}
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleting(null)}>Cancel</Button>
            <button
              type="button"
              onClick={confirmDelete}
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-red-700"
            >
              <Trash2 size={14} /> Delete
            </button>
          </>
        }
      >
        <p className="text-sm text-ink-600 dark:text-ink-300">
          This can&apos;t be undone. Their automations, contacts, and logs remain in the database but they lose access entirely.
        </p>
      </Modal>
    </div>
  );
};

export default AdminUsers;