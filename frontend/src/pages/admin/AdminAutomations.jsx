import { useEffect, useMemo, useState } from 'react';
import { Search, Workflow, Trash2 } from 'lucide-react';
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
import Dropdown from '../../components/ui/Dropdown.jsx';
import adminService from '../../services/adminService.js';
import { TRIGGER_LABEL } from '../../utils/constants.js';

const STATUS_FILTERS = [
  { value: 'all', label: 'All' },
  { value: 'enabled', label: 'Active' },
  { value: 'paused', label: 'Paused' },
];

const AdminAutomations = () => {
  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [deleting, setDeleting] = useState(null);

  const load = async () => {
    setIsLoading(true);
    try {
      const data = await adminService.listAutomations();
      setItems(data);
    } catch {
      /* interceptor toasted */
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return items.filter((a) => {
      if (statusFilter === 'enabled' && !a.enabled) return false;
      if (statusFilter === 'paused' && a.enabled) return false;
      if (!q) return true;
      return (
        (a.name || '').toLowerCase().includes(q) ||
        (a.ownerEmail || '').toLowerCase().includes(q)
      );
    });
  }, [items, query, statusFilter]);

  const handleToggle = async (a) => {
    const optimistic = items.map((x) => x.id === a.id ? { ...x, enabled: !x.enabled } : x);
    setItems(optimistic);
    try {
      await adminService.toggleAutomation(a.uid, a.id);
    } catch {
      setItems(items);
    }
  };

  const confirmDelete = async () => {
    if (!deleting) return;
    try {
      await adminService.deleteAutomation(deleting.uid, deleting.id);
      setItems((s) => s.filter((a) => a.id !== deleting.id));
      toast.success('Automation deleted.');
    } catch {
      /* interceptor toasted */
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div>
      <PageHeader title="Automations" description="Every automation across every user." />

      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name or owner email…"
            className="input pl-9"
          />
        </div>
        <Dropdown
          label="Status"
          options={STATUS_FILTERS}
          value={statusFilter}
          onChange={setStatusFilter}
          align="right"
        />
      </div>

      <Card padded={false}>
        {isLoading ? (
          <div className="space-y-3 p-5">
            {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
          </div>
        ) : filtered.length === 0 ? (
          <div className="p-8">
            <EmptyState icon={Workflow} title="No automations found" description="Try a different search or filter." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-ink-100 text-left text-xs uppercase tracking-wider text-ink-400 dark:border-ink-800">
                  <th className="px-5 py-3 font-medium">Automation</th>
                  <th className="px-5 py-3 font-medium">Owner</th>
                  <th className="px-5 py-3 font-medium">Trigger</th>
                  <th className="px-5 py-3 font-medium">Runs / Success</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100 dark:divide-ink-800">
                {filtered.map((a) => (
                  <tr key={a.id} className="row-hover">
                    <td className="px-5 py-3 font-medium text-ink-900 dark:text-ink-100">{a.name || '—'}</td>
                    <td className="px-5 py-3 text-ink-600 dark:text-ink-300">{a.ownerEmail || '—'}</td>
                    <td className="px-5 py-3 text-ink-600 dark:text-ink-300">{TRIGGER_LABEL[a.trigger] || a.trigger}</td>
                    <td className="px-5 py-3 text-ink-600 dark:text-ink-300">
                      {a.runCount} / {a.successCount}
                    </td>
                    <td className="px-5 py-3">
                      <Badge tone={a.enabled ? 'success' : 'neutral'} dot>
                        {a.enabled ? 'Active' : 'Paused'}
                      </Badge>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <Switch checked={a.enabled} onChange={() => handleToggle(a)} size="sm" />
                        <IconButton tone="danger" size="sm" aria-label="Delete" onClick={() => setDeleting(a)}>
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
        title="Delete automation?"
        description={deleting ? `"${deleting.name}" owned by ${deleting.ownerEmail} will be permanently removed.` : ''}
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
        <p className="text-sm text-ink-600 dark:text-ink-300">This can&apos;t be undone.</p>
      </Modal>
    </div>
  );
};

export default AdminAutomations;