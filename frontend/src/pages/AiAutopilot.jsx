import { useEffect, useState, useCallback } from 'react';
import { Bot, Save, Lock, Info, MessageCircle, Clock, Users, AlertTriangle, Plus, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';

import PageHeader from '../components/ui/PageHeader.jsx';
import StatCard from '../components/ui/StatCard.jsx';
import Button from '../components/form/Button.jsx';
import FormField from '../components/form/FormField.jsx';
import Checkbox from '../components/form/Checkbox.jsx';
import Switch from '../components/ui/Switch.jsx';
import autopilotService from '../services/autopilotService.js';
import automationService from '../services/automationService.js';
import { ROUTES } from '../utils/constants.js';
import { useAccountStore } from '../store/accountStore.js';

const ROLES = [
  { value: 'SALES_ASSISTANT', label: 'Sales Assistant' },
  { value: 'CUSTOMER_SUPPORT', label: 'Customer Support' },
  { value: 'COACH', label: 'Coach / Consultant' },
  { value: 'CUSTOM', label: 'Custom' },
];

const DEFAULT_ACTIONS = {
  collectEmail: true,
  collectPhone: false,
  recommendProducts: true,
  updateContacts: true,
  addTags: true,
  notifyOwner: true,
  escalateToHuman: true,
  sendTemplates: false,
  triggerAutomations: false,
};

const MAX_TEMPLATES = 20;
const emptyTemplate = () => ({ id: '', label: '', description: '', message: '' });

const AiAutopilot = () => {
  const activeAccount = useAccountStore((s) => s.activeAccount);

  const [enabled, setEnabled] = useState(false);
  const [role, setRole] = useState('SALES_ASSISTANT');
  const [systemPrompt, setSystemPrompt] = useState('');
  const [goal, setGoal] = useState('');
  const [tone, setTone] = useState('friendly');
  const [allowedActions, setAllowedActions] = useState(DEFAULT_ACTIONS);
  const [conversationTimeoutMinutes, setConversationTimeoutMinutes] = useState(30);
  const [fallbackMessage, setFallbackMessage] = useState('');
  const [messageTemplates, setMessageTemplates] = useState([]);
  const [allowedAutomationIds, setAllowedAutomationIds] = useState([]);
  const [automations, setAutomations] = useState([]);
  const [planEligible, setPlanEligible] = useState(true);
  const [plan, setPlan] = useState('FREE');

  const [stats, setStats] = useState(null);

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await autopilotService.get();
      setEnabled(!!data.enabled);
      setRole(data.role || 'SALES_ASSISTANT');
      setSystemPrompt(data.systemPrompt || '');
      setGoal(data.goal || '');
      setTone(data.tone || 'friendly');
      setAllowedActions({ ...DEFAULT_ACTIONS, ...(data.allowedActions || {}) });
      setConversationTimeoutMinutes(data.conversationTimeoutMinutes || 30);
      setFallbackMessage(data.fallbackMessage || '');
      setMessageTemplates(data.messageTemplates && data.messageTemplates.length > 0 ? data.messageTemplates : []);
      setAllowedAutomationIds(data.allowedAutomationIds || []);
      setPlanEligible(!!data.planEligible);
      setPlan(data.plan || 'FREE');

      try {
        const s = await autopilotService.stats();
        setStats(s);
      } catch {
        setStats(null);
      }

      try {
        const list = await automationService.list();
        setAutomations(list || []);
      } catch {
        setAutomations([]);
      }
    } catch (err) {
      setError('Failed to load AI Autopilot settings.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load, activeAccount?.instagramUserId]);

  const handleActionToggle = (key) => {
    setAllowedActions((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleTemplateChange = (index, field, value) => {
    setMessageTemplates((prev) => prev.map((t, i) => (i === index ? { ...t, [field]: value } : t)));
  };

  const handleAddTemplate = () => {
    if (messageTemplates.length >= MAX_TEMPLATES) return;
    setMessageTemplates((prev) => [...prev, emptyTemplate()]);
  };

  const handleRemoveTemplate = (index) => {
    setMessageTemplates((prev) => prev.filter((_, i) => i !== index));
  };

  const handleAutomationToggle = (id) => {
    setAllowedAutomationIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  };

  const handleSave = async () => {
    if (!planEligible) return;

    setIsSaving(true);
    try {
      const payload = {
        enabled,
        role,
        systemPrompt: systemPrompt.slice(0, 4000),
        goal: goal.slice(0, 500),
        tone,
        allowedActions,
        conversationTimeoutMinutes: Math.max(5, Math.min(conversationTimeoutMinutes, 24 * 60)),
        fallbackMessage: fallbackMessage.slice(0, 500),
        messageTemplates: messageTemplates
          .filter((t) => t.label.trim().length > 0 && t.message.trim().length > 0)
          .map((t) => ({
            id: t.id || undefined,
            label: t.label.trim(),
            description: t.description?.trim() || '',
            message: t.message.trim(),
          })),
        allowedAutomationIds,
      };
      await autopilotService.save(payload);
      toast.success('AI Autopilot settings saved.');
      await load();
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to save AI Autopilot settings.');
    } finally {
      setIsSaving(false);
    }
  };

  if (!activeAccount) {
    return (
      <div className="mx-auto max-w-2xl">
        <PageHeader
          title="AI Autopilot"
          description="A full AI sales & support agent that manages conversations for you."
        />
        <div className="card p-8 text-center">
          <Bot size={36} className="mx-auto mb-3 text-ink-300 dark:text-ink-600" />
          <p className="text-sm text-ink-500 dark:text-ink-400">
            Connect an Instagram account first to set up AI Autopilot.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="AI Autopilot"
        description="Holds full multi-turn conversations, qualifies leads, and can escalate to you — separate from AI FAQ."
        actions={
          <Button
            leftIcon={Save}
            onClick={handleSave}
            isLoading={isSaving}
            disabled={isLoading || !planEligible}
          >
            Save
          </Button>
        }
      />

      {!isLoading && !planEligible && (
        <div className="mb-6 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/60 p-4 dark:border-amber-800 dark:bg-amber-500/10">
          <Lock size={18} className="mt-0.5 shrink-0 text-amber-600 dark:text-amber-400" />
          <div className="flex-1">
            <p className="text-sm font-medium text-amber-800 dark:text-amber-300">
              AI Autopilot is a Pro feature.
            </p>
            <p className="mt-0.5 text-sm text-amber-700 dark:text-amber-400">
              You're on the {plan === 'FREE' ? 'Free' : plan} plan. Upgrade to Pro to enable full AI conversations.
            </p>
            <Link
              to={ROUTES.SUBSCRIPTION}
              className="mt-2 inline-block text-sm font-semibold text-amber-800 underline dark:text-amber-300"
            >
              View plans &rarr;
            </Link>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <div
              key={i}
              className="h-20 animate-pulse rounded-2xl border border-ink-100 bg-ink-50 dark:border-ink-800 dark:bg-ink-800/30"
            />
          ))}
        </div>
      ) : error ? (
        <div className="card p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
          <Button variant="secondary" className="mt-4" onClick={load}>
            Retry
          </Button>
        </div>
      ) : (
        <>
          {stats && (
            <div className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
              <StatCard label="Conversations" value={stats.conversationCount} icon={MessageCircle} tone="brand" />
              <StatCard label="Avg response" value={`${(stats.avgResponseTimeMs / 1000).toFixed(1)}s`} icon={Clock} tone="neutral" />
              <StatCard label="Contacts handled" value={stats.contactsHandled} icon={Users} tone="success" />
              <StatCard label="Escalations" value={stats.escalations} icon={AlertTriangle} tone="warning" />
            </div>
          )}

          <fieldset disabled={!planEligible} className={!planEligible ? 'opacity-60' : ''}>
            <div className="card mb-4 flex items-center justify-between p-4">
              <div>
                <p className="text-sm font-semibold text-ink-900 dark:text-ink-100">Enable AI Autopilot</p>
                <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">
                  Takes over full conversations. Runs instead of AI FAQ when enabled.
                </p>
              </div>
              <Switch checked={enabled} onChange={setEnabled} srLabel="Enable AI Autopilot" />
            </div>

            <div className="mb-4 flex items-start gap-2 rounded-xl bg-ink-50/60 px-3 py-2.5 text-xs text-ink-500 dark:bg-ink-800/30 dark:text-ink-400">
              <Info size={14} className="mt-0.5 shrink-0" />
              <p>
                When enabled, Autopilot handles unmatched DMs instead of AI FAQ for this account, so the two never
                answer the same message. It reuses the knowledge base &amp; Q&amp;A from your AI FAQ page.
              </p>
            </div>

            <div className="card mb-4 p-4">
              <label className="label mb-2 block">AI role</label>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                {ROLES.map((r) => (
                  <button
                    key={r.value}
                    type="button"
                    onClick={() => setRole(r.value)}
                    className={`rounded-xl border px-3 py-2 text-xs font-medium transition-colors ${
                      role === r.value
                        ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
                        : 'border-ink-200 text-ink-500 hover:border-brand-300 dark:border-ink-700 dark:text-ink-400'
                    }`}
                  >
                    {r.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="card mb-4 p-4">
              <label className="label">Goal</label>
              <input
                className="input"
                placeholder="e.g. Get the customer to book a call, or complete a purchase"
                value={goal}
                maxLength={500}
                onChange={(e) => setGoal(e.target.value)}
              />
            </div>

            <div className="card mb-4 p-4">
              <label className="label">Tone</label>
              <input
                className="input"
                placeholder="e.g. friendly and casual, professional, energetic"
                value={tone}
                onChange={(e) => setTone(e.target.value)}
              />
            </div>

            <div className="card mb-4 p-4">
              <label className="label">Business instructions (system prompt)</label>
              <textarea
                className="input min-h-[140px] resize-y"
                placeholder="Describe your business, products, and how you want Autopilot to sell or support customers..."
                value={systemPrompt}
                maxLength={4000}
                onChange={(e) => setSystemPrompt(e.target.value)}
              />
              <p className="mt-1 text-right text-xs text-ink-400 dark:text-ink-500">
                {4000 - systemPrompt.length} chars left
              </p>
            </div>

            <div className="card mb-4 p-4">
              <p className="mb-3 text-sm font-semibold text-ink-900 dark:text-ink-100">Allowed actions</p>
              <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                <Checkbox
                  label="Collect email"
                  checked={allowedActions.collectEmail}
                  onChange={() => handleActionToggle('collectEmail')}
                />
                <Checkbox
                  label="Collect phone"
                  checked={allowedActions.collectPhone}
                  onChange={() => handleActionToggle('collectPhone')}
                />
                <Checkbox
                  label="Recommend products"
                  checked={allowedActions.recommendProducts}
                  onChange={() => handleActionToggle('recommendProducts')}
                />
                <Checkbox
                  label="Update contacts"
                  checked={allowedActions.updateContacts}
                  onChange={() => handleActionToggle('updateContacts')}
                />
                <Checkbox
                  label="Add tags"
                  checked={allowedActions.addTags}
                  onChange={() => handleActionToggle('addTags')}
                />
                <Checkbox
                  label="Notify owner"
                  checked={allowedActions.notifyOwner}
                  onChange={() => handleActionToggle('notifyOwner')}
                />
                <Checkbox
                  label="Escalate to human"
                  checked={allowedActions.escalateToHuman}
                  onChange={() => handleActionToggle('escalateToHuman')}
                />
                <Checkbox
                  label="Send predefined DM templates"
                  checked={allowedActions.sendTemplates}
                  onChange={() => handleActionToggle('sendTemplates')}
                />
                <Checkbox
                  label="Trigger selected automations"
                  checked={allowedActions.triggerAutomations}
                  onChange={() => handleActionToggle('triggerAutomations')}
                />
              </div>

              <div className="mt-3 border-t border-ink-100 pt-3 dark:border-ink-800">
                <p className="mb-2 text-xs font-medium text-ink-400 dark:text-ink-500">
                  Never available to AI Autopilot, by design:
                </p>
                <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                  <Checkbox label="Delete contacts" checked={false} disabled />
                  <Checkbox label="Run admin actions" checked={false} disabled />
                </div>
              </div>
            </div>

            {allowedActions.sendTemplates && (
              <div className="card mb-4 p-4">
                <p className="mb-1 text-sm font-semibold text-ink-900 dark:text-ink-100">Message templates</p>
                <p className="mb-3 text-xs text-ink-500 dark:text-ink-400">
                  Write the exact wording once (lead magnet link, coupon code, booking link...). The AI will send
                  one of these verbatim when it clearly applies, instead of writing its own text.
                </p>
                <div className="space-y-3">
                  {messageTemplates.map((t, index) => (
                    <div key={index} className="rounded-xl border border-ink-100 p-3 dark:border-ink-800">
                      <div className="mb-2 flex items-start gap-3">
                        <div className="min-w-0 flex-1 space-y-2">
                          <FormField
                            label="Label"
                            placeholder='e.g. "Free Guide"'
                            value={t.label}
                            onChange={(e) => handleTemplateChange(index, 'label', e.target.value)}
                          />
                          <FormField
                            label="When should the AI use this? (optional hint)"
                            placeholder='e.g. "Customer asks for a free resource or guide"'
                            value={t.description}
                            onChange={(e) => handleTemplateChange(index, 'description', e.target.value)}
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => handleRemoveTemplate(index)}
                          className="shrink-0 self-start rounded-lg p-1.5 text-ink-400 transition-colors hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-900/20"
                          title="Remove"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                      <label className="label">Exact message to send</label>
                      <textarea
                        className="input min-h-[70px] resize-y"
                        placeholder="e.g. Here's your free guide: [link]. Let me know if you have questions!"
                        value={t.message}
                        maxLength={900}
                        onChange={(e) => handleTemplateChange(index, 'message', e.target.value)}
                      />
                    </div>
                  ))}

                  {messageTemplates.length < MAX_TEMPLATES && (
                    <button
                      type="button"
                      onClick={handleAddTemplate}
                      className="flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-ink-200 py-3 text-sm font-medium text-ink-400 transition-colors hover:border-brand-400 hover:text-brand-600 dark:border-ink-700 dark:text-ink-500 dark:hover:border-brand-600 dark:hover:text-brand-400"
                    >
                      <Plus size={16} />
                      Add template ({messageTemplates.length}/{MAX_TEMPLATES})
                    </button>
                  )}
                </div>
              </div>
            )}

            {allowedActions.triggerAutomations && (
              <div className="card mb-4 p-4">
                <p className="mb-1 text-sm font-semibold text-ink-900 dark:text-ink-100">Automations AI can trigger</p>
                <p className="mb-3 text-xs text-ink-500 dark:text-ink-400">
                  Pick which of your existing automations the AI is allowed to invoke mid-conversation. It can
                  never trigger anything not selected here.
                </p>
                {automations.length === 0 ? (
                  <p className="text-sm text-ink-400 dark:text-ink-500">
                    You don't have any automations yet for this account.
                  </p>
                ) : (
                  <div className="space-y-2">
                    {automations.map((a) => (
                      <label
                        key={a.id}
                        className="flex items-center justify-between gap-3 rounded-xl border border-ink-100 px-3 py-2.5 text-sm dark:border-ink-800"
                      >
                        <span className="flex-1 truncate text-ink-700 dark:text-ink-300">
                          {a.name || 'Untitled automation'}
                          {!a.enabled && (
                            <span className="ml-2 text-xs text-ink-400 dark:text-ink-500">(currently off)</span>
                          )}
                        </span>
                        <input
                          type="checkbox"
                          className="h-4 w-4 shrink-0 rounded border-ink-300 text-brand-600 focus:ring-brand-500"
                          checked={allowedAutomationIds.includes(a.id)}
                          onChange={() => handleAutomationToggle(a.id)}
                        />
                      </label>
                    ))}
                  </div>
                )}
              </div>
            )}

            <div className="card mb-4 p-4">
              <FormField
                label="Conversation timeout (minutes)"
                type="number"
                min={5}
                max={1440}
                value={conversationTimeoutMinutes}
                onChange={(e) => setConversationTimeoutMinutes(Number(e.target.value) || 30)}
              />
              <p className="mt-1 text-xs text-ink-500 dark:text-ink-400">
                A new conversation starts fresh after this many minutes of inactivity.
              </p>
            </div>

            <div className="card p-4">
              <label className="label">Fallback / escalation message</label>
              <textarea
                className="input min-h-[80px] resize-y"
                placeholder="e.g. Thanks for reaching out! I'll have someone from the team follow up with you shortly."
                value={fallbackMessage}
                maxLength={500}
                onChange={(e) => setFallbackMessage(e.target.value)}
              />
              <p className="mt-1 text-xs text-ink-500 dark:text-ink-400">
                Sent when Autopilot escalates a conversation to you instead of replying itself.
              </p>
            </div>
          </fieldset>
        </>
      )}
    </div>
  );
};

export default AiAutopilot;
