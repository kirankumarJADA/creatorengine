import { useState } from 'react';
import { Play, CheckCircle2, XCircle, AtSign, MessageSquare } from 'lucide-react';
import Modal from '../ui/Modal.jsx';
import Button from '../form/Button.jsx';
import FormField from '../form/FormField.jsx';
import TextArea from '../ui/TextArea.jsx';
import Badge from '../ui/Badge.jsx';
import DmPreview from './DmPreview.jsx';
import { simulateRun } from '../../utils/automationEngine.js';
import { TRIGGER_LABEL, ACTION_TYPE } from '../../utils/constants.js';

/**
 * Mock-trigger simulator.
 *
 * Lets the user fire a hypothetical Instagram event at a saved
 * automation and see whether it would match plus what would happen.
 * Pure client-side — no backend roundtrip — so it's safe to spam.
 *
 * The simulator deliberately ignores `enabled`-state for the run
 * itself (it runs in "dry-run" mode), but warns when matching would
 * otherwise be blocked by the toggle being off.
 */
const SimulatorModal = ({ open, onClose, automation }) => {
  const [username, setUsername] = useState('test.user');
  const [content,  setContent]  = useState('I want the link!');
  const [result,   setResult]   = useState(null);

  if (!automation) return null;

  const handleRun = () => {
    // Force-enable for the simulation so the user is testing
    // matching logic, not the toggle.
    const dryAutomation = { ...automation, enabled: true };
    const event = { type: automation.trigger, content, username };
    setResult(simulateRun(dryAutomation, event));
  };

  const handleClose = () => {
    setResult(null);
    onClose?.();
  };

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title="Test automation"
      description={`Fire a fake "${TRIGGER_LABEL[automation.trigger]}" event and see what happens.`}
      size="lg"
      footer={
        <>
          <Button variant="secondary" onClick={handleClose}>Close</Button>
          <Button leftIcon={Play} onClick={handleRun}>Run test</Button>
        </>
      }
    >
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        {/* Input pane */}
        <div className="space-y-4">
          <div className="rounded-xl border border-ink-100 bg-ink-50/60 px-3 py-2 text-xs text-ink-600 dark:border-ink-800 dark:bg-ink-800/40 dark:text-ink-300">
            <p>
              <span className="font-semibold">Automation:</span>{' '}
              {automation.name || `${TRIGGER_LABEL[automation.trigger]} → ${automation.action?.type}`}
            </p>
            {!automation.enabled && (
              <p className="mt-1 text-amber-700 dark:text-amber-400">
                ⚠ This automation is paused. Test is running in dry-run mode.
              </p>
            )}
          </div>

          <FormField
            label="Sender username"
            leftIcon={AtSign}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="follower"
            hint="Used to substitute {{username}} in your message."
          />

          <TextArea
            label="Event content"
            rows={4}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={`Type a fake "${TRIGGER_LABEL[automation.trigger]}" body…`}
            hint="What the user would have said in the comment, DM, or story reply."
            maxLength={500}
            showCount
          />
        </div>

        {/* Output pane */}
        <div className="space-y-4">
          {result == null ? (
            <div className="flex h-full min-h-[200px] flex-col items-center justify-center rounded-2xl border border-dashed border-ink-200 bg-ink-50/50 text-center text-sm text-ink-500 dark:border-ink-800 dark:bg-ink-800/30 dark:text-ink-400">
              <MessageSquare size={24} className="mb-2 opacity-60" />
              <p>Click <strong>Run test</strong> to see the result.</p>
            </div>
          ) : result.matched ? (
            <div>
              <div className="mb-3 flex items-center gap-2">
                <Badge tone="success" dot>Matched</Badge>
                <p className="text-xs text-ink-500 dark:text-ink-400">
                  {result.reason}
                </p>
              </div>
              <DmPreview
                message={result.output.message}
                recipientHandle={result.output.to}
                link={
                  automation.action?.type === ACTION_TYPE.SEND_LINK
                    ? automation.action?.link
                    : null
                }
              />
              <div className="mt-3 rounded-xl border border-emerald-100 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-300">
                <CheckCircle2 size={14} className="-mt-0.5 mr-1 inline" />
                {result.output.kind}
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-red-100 bg-red-50 p-4 dark:border-red-500/20 dark:bg-red-500/10">
              <div className="flex items-center gap-2">
                <Badge tone="danger" dot>No match</Badge>
              </div>
              <p className="mt-2 text-sm text-red-800 dark:text-red-300">
                <XCircle size={14} className="-mt-0.5 mr-1 inline" />
                {result.reason}
              </p>
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
};

export default SimulatorModal;
