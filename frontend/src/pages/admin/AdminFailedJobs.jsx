import { useEffect, useState } from 'react';
import { RefreshCw, Trash2, AlertOctagon } from 'lucide-react';
import toast from 'react-hot-toast';

import PageHeader from '../../components/ui/PageHeader.jsx';
import { Card } from '../../components/ui/Card.jsx';
import IconButton from '../../components/ui/IconButton.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import adminService from '../../services/adminService.js';
import { formatRelative } from '../../utils/helpers.js';

const AdminFailedJobs = () => {
  const [jobs, setJobs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);

  const load = async () => {
    setIsLoading(true);
    try {
      const data = await adminService.listFailedJobs(300);
      setJobs(data);
    } catch {
      /* interceptor toasted */
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleRetry = async (job) => {
    setBusyId(job.id);
    try {
      await adminService.retryFailedJob(job.uid, job.id);
      setJobs((s) => s.filter((j) => j.id !== job.id));
      toast.success('Retry queued.');
    } catch {
      /* interceptor toasted */
    } finally {
      setBusyId(null);
    }
  };

  const handleDelete = async (job) => {
    setBusyId(job.id);
    try {
      await adminService.deleteFailedJob(job.uid, job.id);
      setJobs((s) => s.filter((j) => j.id !== job.id));
      toast.success('Failed job deleted.');
    } catch {
      /* interceptor toasted */
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <PageHeader title="Failed Jobs" description="Dead-lettered automation runs across every user." />

      <Card padded={false}>
        {isLoading ? (
          <div className="space-y-3 p-5">
            {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
          </div>
        ) : jobs.length === 0 ? (
          <div className="p-8">
            <EmptyState icon={AlertOctagon} title="No failed jobs" description="Everything is running cleanly." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-ink-100 text-left text-xs uppercase tracking-wider text-ink-400 dark:border-ink-800">
                  <th className="px-5 py-3 font-medium">Owner</th>
                  <th className="px-5 py-3 font-medium">Automation</th>
                  <th className="px-5 py-3 font-medium">Reason</th>
                  <th className="px-5 py-3 font-medium">Attempts</th>
                  <th className="px-5 py-3 font-medium">When</th>
                  <th className="px-5 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100 dark:divide-ink-800">
                {jobs.map((j) => (
                  <tr key={j.id} className="row-hover">
                    <td className="px-5 py-3 text-ink-800 dark:text-ink-200">{j.ownerEmail || '—'}</td>
                    <td className="px-5 py-3 text-ink-600 dark:text-ink-300">{j.automationName || '—'}</td>
                    <td className="max-w-xs truncate px-5 py-3 text-ink-500 dark:text-ink-400" title={j.reason}>
                      {j.reason || '—'}
                    </td>
                    <td className="px-5 py-3 text-ink-600 dark:text-ink-300">{j.attempts}</td>
                    <td className="px-5 py-3 text-ink-500 dark:text-ink-400">{formatRelative(j.timestamp)}</td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-1">
                        <IconButton
                          size="sm"
                          aria-label="Retry"
                          onClick={() => handleRetry(j)}
                          disabled={busyId === j.id}
                        >
                          <RefreshCw size={14} className={busyId === j.id ? 'animate-spin' : ''} />
                        </IconButton>
                        <IconButton
                          size="sm"
                          tone="danger"
                          aria-label="Delete"
                          onClick={() => handleDelete(j)}
                          disabled={busyId === j.id}
                        >
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
    </div>
  );
};

export default AdminFailedJobs;