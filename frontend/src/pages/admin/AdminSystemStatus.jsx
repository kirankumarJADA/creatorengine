import { useEffect, useState } from 'react';
import { Server, Database, Webhook, Users, Send, ExternalLink } from 'lucide-react';
import PageHeader from '../../components/ui/PageHeader.jsx';
import { Card } from '../../components/ui/Card.jsx';
import Badge from '../../components/ui/Badge.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import adminService from '../../services/adminService.js';

const RENDER_LOGS_URL = 'https://dashboard.render.com/web/srv-d88sup99rddc738ptrv0/logs';

const StatusRow = ({ icon: Icon, label, value, tone }) => (
  <div className="flex items-center justify-between border-b border-ink-100 py-4 last:border-0 dark:border-ink-800">
    <div className="flex items-center gap-3">
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300">
        <Icon size={16} />
      </span>
      <span className="text-sm font-medium text-ink-800 dark:text-ink-200">{label}</span>
    </div>
    {tone ? (
      <Badge tone={tone} dot>{value}</Badge>
    ) : (
      <span className="text-sm font-semibold text-ink-900 dark:text-ink-100">{value}</span>
    )}
  </div>
);

const AdminSystemStatus = () => {
  const [status, setStatus] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    adminService.getSystemStatus()
      .then((data) => { if (!cancelled) setStatus(data); })
      .catch(() => {})
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, []);

  return (
    <div>
      <PageHeader title="System Status" description="Live backend health checks." />

      <Card>
        {isLoading ? (
          <div className="space-y-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : (
          <>
            <StatusRow
              icon={Server}
              label="Backend API"
              value={status?.apiStatus === 'UP' ? 'Healthy' : 'Down'}
              tone={status?.apiStatus === 'UP' ? 'success' : 'danger'}
            />
            <StatusRow
              icon={Database}
              label="Database (Firestore)"
              value={status?.databaseStatus === 'UP' ? 'Healthy' : 'Down'}
              tone={status?.databaseStatus === 'UP' ? 'success' : 'danger'}
            />
            <StatusRow
              icon={Webhook}
              label="Instagram webhook"
              value={status?.webhookConfigured || 'Unknown'}
              tone="brand"
            />
            <StatusRow
              icon={Users}
              label="Active users (last 30 min)"
              value={status?.activeUsersLast30Min ?? 0}
            />
            <StatusRow
              icon={Send}
              label="Total DMs sent (all-time)"
              value={status?.totalDmsSent ?? 0}
            />
          </>
        )}

        <a
          href={RENDER_LOGS_URL}
          target="_blank"
          rel="noreferrer"
          className="mt-5 flex items-center justify-center gap-2 rounded-xl border border-ink-200 px-4 py-3 text-sm font-medium text-ink-700 transition-colors hover:bg-ink-50 dark:border-ink-800 dark:text-ink-200 dark:hover:bg-ink-800"
        >
          <ExternalLink size={15} />
          View Render Logs & Deployments
        </a>
      </Card>
    </div>
  );
};

export default AdminSystemStatus;