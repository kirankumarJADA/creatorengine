import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, Search, Workflow, Pencil, Trash2,
  MessageSquare, AtSign, Send, Link2, UserCheck,
  PlayCircle, ChevronLeft, ChevronRight, ArrowRight, Clock,
} from 'lucide-react';
import toast from 'react-hot-toast';

import PageHeader from '../components/ui/PageHeader.jsx';
import { Card } from '../components/ui/Card.jsx';
import Badge from '../components/ui/Badge.jsx';
import Dropdown from '../components/ui/Dropdown.jsx';
import Switch from '../components/ui/Switch.jsx';
import IconButton from '../components/ui/IconButton.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import Modal from '../components/ui/Modal.jsx';
import Button from '../components/form/Button.jsx';
import SimulatorModal from '../components/builder/SimulatorModal.jsx';

import { useAutomationStore } from '../store/automationStore.js';
import {
  ROUTES, buildRoute,
  TRIGGER_TYPE, TRIGGER_LABEL,
  CONDITION_TYPE, ACTION_TYPE, ACTION_LABEL, MATCH_TYPE,
  PAGE_SIZE,
} from '../utils/constants.js';
import { cn } from '../utils/helpers.js';

const TRIGGER_ICON = {
  [TRIGGER_TYPE.COMMENT]:     MessageSquare,
  [TRIGGER_TYPE.DM]:          Send,
  [TRIGGER_TYPE.STORY_REPLY]: AtSign,
  [TRIGGER_TYPE.NEXT_POST]:   Clock,
};
const ACTION_ICON = {
  [ACTION_TYPE.SEND_DM]:      Send,
  [ACTION_TYPE.SEND_MESSAGE]: MessageSquare,
  [ACTION_TYPE.SEND_LINK]:    Link2,
  [ACTION_TYPE.SAVE_CONTACT]: UserCheck,
};
const STATUS_FILTERS = [
  { value: 'all',     label: 'All' },
  { value: 'enabled', label: 'Active' },
  { value: 'paused',  label: 'Paused' },
];
const TRIGGER_FILTERS = [
  { value: 'all', label: 'All triggers' },
  ...Object.values(TRIGGER_TYPE).map((t) => ({ value: t, label: TRIGGER_LABEL[t] })),
];

const Automations = () => {
  const navigate = useNavigate();

  const automations       = useAutomationStore((s) => s.automations);
  const fetchAutomations  = useAutomationStore((s) => s.fetchAutomations);
  const deleteAutomation  = useAutomationStore((s) => s.deleteAutomation);
  const toggleAutomation  = useAutomationStore((s) => s.toggleAutomation);

  const [query, setQuery]                 = useState('');
  const [statusFilter, setStatusFilter]   = useState('all');
  const [triggerFilter, setTriggerFilter] = useState('all');
  const [page, setPage]                   = useState(1);
  const [deletingId, setDeletingId]       = useState(null);
  const [testingAutomation, setTesting]   = useState(null);

  useEffect(() => { fetchAutomations(); }, [fetchAutomations]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return automations.filter((a) => {
      if (statusFilter === 'enabled' && !a.enabled) return false;
      if (statusFilter === 'paused'  &&  a.enabled) return false;
      if (triggerFilter !== 'all' && a.trigger !== triggerFilter) return false;
      if (!q) return true;
      return (
        (a.name || '').toLowerCase().includes(q) ||
        TRIGGER_LABEL[a.trigger]?.toLowerCase().includes(q) ||
        (a.condition?.keyword || '').toLowerCase().includes(q)
      );
    });
  }, [automations, query, statusFilter, triggerFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => { if (page > totalPages) setPage(totalPages); }, [totalPages, page]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  const handleCreate = () => navigate(ROUTES.AUTOMATION_NEW);
  const handleEdit   = (id) => navigate(buildRoute.automationEdit(id));
  const handleToggle = (id) => toggleAutomation(id);

  const confirmDelete = async () => {
    if (!deletingId) return;
    await deleteAutomation(deletingId);
    toast.success('Automation deleted.');
    setDeletingId(null);
  };

  const target = automations.find((a) => a.id === deletingId);

  return (
    <div>
      <PageHeader
        title="Automations"
        description="Build and manage your Instagram workflows."
        actions={<Button leftIcon={Plus} onClick={handleCreate}>Create automation</Button>}
      />

      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
          <input
            type="search"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setPage(1); }}
            placeholder="Search by name, trigger, or keyword…"
            className="input pl-9"
          />
        </div>
        <Dropdown
          label="Trigger"
          options={TRIGGER_FILTERS}
          value={triggerFilter}
          onChange={(v) => { setTriggerFilter(v); setPage(1); }}
          align="right"
        />
        <Dropdown
          label="Status"
          options={STATUS_FILTERS}
          value={statusFilter}
          onChange={(v) => { setStatusFilter(v); setPage(1); }}
          align="right"
        />
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={Workflow}
          title={query || statusFilter !== 'all' || triggerFilter !== 'all'
            ? 'No matches' : 'No automations yet'}
          description={
            query || statusFilter !== 'all' || triggerFilter !== 'all'
              ? 'Try a different search or clear the filter.'
              : 'Create your first automation to start engaging followers.'
          }
          action={<Button leftIcon={Plus} onClick={handleCreate}>Create automation</Button>}
        />
      ) : (
        <>
          <motion.div layout className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <AnimatePresence>
              {paginated.map((a, i) => (
                <AutomationCard
                  key={a.id}
                  automation={a}
                  index={i}
                  onToggle={() => handleToggle(a.id)}
                  onEdit={() => handleEdit(a.id)}
                  onDelete={() => setDeletingId(a.id)}
                  onTest={() => setTesting(a)}
                />
              ))}
            </AnimatePresence>
          </motion.div>

          {totalPages > 1 && (
            <Pagination
              page={page}
              totalPages={totalPages}
              total={filtered.length}
              pageSize={PAGE_SIZE}
              onChange={setPage}
            />
          )}
        </>
      )}

      <Modal
        open={Boolean(deletingId)}
        onClose={() => setDeletingId(null)}
        title="Delete automation?"
        description={target?.name
          ? `"${target.name}" will be removed permanently.`
          : 'This automation will be removed permanently.'}
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeletingId(null)}>Cancel</Button>
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
          This can&apos;t be undone. If you just want to stop it temporarily,
          use the toggle on the card instead.
        </p>
      </Modal>

      <SimulatorModal
        open={Boolean(testingAutomation)}
        onClose={() => setTesting(null)}
        automation={testingAutomation}
      />
    </div>
  );
};

const AutomationCard = ({ automation, onToggle, onEdit, onDelete, onTest, index = 0 }) => {
  const Icon = TRIGGER_ICON[automation.trigger] || MessageSquare;
  const ActionIcon = ACTION_ICON[automation.action?.type] || Send;
  const isActive = !!automation.enabled;

  const conditionLabel = automation.condition?.type === CONDITION_TYPE.KEYWORD
    ? `${automation.condition.matchType === MATCH_TYPE.EXACT ? '=' : '~'} "${automation.condition.keyword || ''}"`
    : 'Any event';

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.96 }}
      whileHover={{ y: -3 }}
      transition={{ duration: 0.25, delay: index * 0.03 }}
    >
      <Card
        className={cn(
          'group relative flex h-full flex-col overflow-hidden transition-shadow hover:shadow-elevated',
          isActive && 'ring-1 ring-brand-500/20'
        )}
      >
        {isActive && (
          <span className="pointer-events-none absolute -right-12 -top-12 h-32 w-32 rounded-full bg-brand-500/[0.07] blur-2xl" />
        )}

        <div className="flex items-start justify-between gap-3">
          <div className="flex min-w-0 items-start gap-3">
            <span className={cn(
              'grid h-10 w-10 shrink-0 place-items-center rounded-xl transition-colors',
              isActive
                ? 'bg-brand-100 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300'
                : 'bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-400'
            )}>
              <Icon size={18} />
            </span>
            <div className="min-w-0">
              <h3 className="truncate text-base font-semibold text-ink-900 dark:text-ink-100">
                {automation.name || TRIGGER_LABEL[automation.trigger]}
              </h3>
              <p className="mt-0.5 truncate text-sm text-ink-500 dark:text-ink-400">
                {TRIGGER_LABEL[automation.trigger]}
              </p>
            </div>
          </div>
          <Badge tone={isActive ? 'success' : 'neutral'} dot>
            {isActive ? 'Active' : 'Paused'}
          </Badge>
        </div>

        <div className="mt-4 flex items-center gap-2 rounded-xl border border-ink-100 bg-ink-50/60 px-3 py-2.5 text-xs dark:border-ink-800 dark:bg-ink-800/30">
          <span className="truncate font-mono text-ink-600 dark:text-ink-300">
            {conditionLabel}
          </span>
          <ArrowRight size={13} className={cn('shrink-0', isActive ? 'text-brand-500' : 'text-ink-400')} />
          <span className="flex shrink-0 items-center gap-1.5 font-medium text-ink-800 dark:text-ink-200">
            <ActionIcon size={12} className="text-ink-500 dark:text-ink-400" />
            {ACTION_LABEL[automation.action?.type]}
          </span>
        </div>

        <div className="mt-auto flex items-center justify-between border-t border-ink-100 pt-4 dark:border-ink-800" style={{ marginTop: 'auto', paddingTop: '1rem' }}>
          <Switch
            checked={isActive}
            onChange={onToggle}
            srLabel={`Toggle ${automation.name || 'automation'}`}
            label={isActive ? 'On' : 'Off'}
            size="sm"
          />
          <div className="flex items-center gap-1 opacity-70 transition-opacity group-hover:opacity-100">
            <IconButton size="sm" aria-label="Test" onClick={onTest}>
              <PlayCircle size={14} />
            </IconButton>
            <IconButton size="sm" aria-label="Edit" onClick={onEdit}>
              <Pencil size={14} />
            </IconButton>
            <IconButton size="sm" tone="danger" aria-label="Delete" onClick={onDelete}>
              <Trash2 size={14} />
            </IconButton>
          </div>
        </div>
      </Card>
    </motion.div>
  );
};

const Pagination = ({ page, totalPages, total, pageSize, onChange }) => {
  const first = (page - 1) * pageSize + 1;
  const last  = Math.min(page * pageSize, total);
  return (
    <nav
      aria-label="Pagination"
      className="mt-6 flex flex-col items-center justify-between gap-3 rounded-2xl border border-ink-100 bg-white px-4 py-3 text-sm dark:border-ink-800 dark:bg-ink-900 sm:flex-row"
    >
      <p className="text-ink-500 dark:text-ink-400">
        Showing <span className="font-medium text-ink-800 dark:text-ink-200">{first}–{last}</span> of {total}
      </p>
      <div className="flex items-center gap-1">
        <IconButton aria-label="Previous page" onClick={() => onChange(Math.max(1, page - 1))} disabled={page <= 1}>
          <ChevronLeft size={16} />
        </IconButton>
        <span className="px-3 text-sm font-medium text-ink-800 dark:text-ink-200">
          {page} <span className="text-ink-400">/ {totalPages}</span>
        </span>
        <IconButton aria-label="Next page" onClick={() => onChange(Math.min(totalPages, page + 1))} disabled={page >= totalPages}>
          <ChevronRight size={16} />
        </IconButton>
      </div>
    </nav>
  );
};

export default Automations;