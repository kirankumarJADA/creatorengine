import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  MessageSquare, Send, AtSign, Zap, X,
  ArrowRight,
} from 'lucide-react';
import Modal from '../ui/Modal.jsx';
import { useBuilderStore } from '../../store/builderStore.js';
import {
  ROUTES,
  TRIGGER_TYPE,
  CONDITION_TYPE,
  MATCH_TYPE,
  ACTION_TYPE,
} from '../../utils/constants.js';
import { cn } from '../../utils/helpers.js';

// ─── Template definitions ────────────────────────────
const TEMPLATES = [
  {
    id: 'comment_to_dm',
    title: 'Comment to DM Flow',
    badge: 'quick',
    badgeTone: 'brand',
    description: 'Automatically reply to comments and send personalised DMs with interactive buttons.',
    icon: MessageSquare,
    iconTone: 'bg-brand-100 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300',
    draft: {
      trigger: TRIGGER_TYPE.COMMENT,
      condition: { type: CONDITION_TYPE.ANY, keyword: '', matchType: MATCH_TYPE.CONTAINS },
      actions: [{ type: ACTION_TYPE.SEND_MESSAGE, message: 'Hey {{username}}! 👋 Thanks for your comment. Here\'s what you asked for:', link: '', variations: [], imageUrl: '', delaySeconds: null }],
    },
  },
  {
    id: 'story_reply',
    title: 'Story Reply Flow',
    badge: 'quick',
    badgeTone: 'brand',
    description: 'Respond to story replies instantly and convert viewers into customers with automated DMs.',
    icon: AtSign,
    iconTone: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
    draft: {
      trigger: TRIGGER_TYPE.STORY_REPLY,
      condition: { type: CONDITION_TYPE.ANY, keyword: '', matchType: MATCH_TYPE.CONTAINS },
      actions: [{ type: ACTION_TYPE.SEND_MESSAGE, message: 'Hey {{username}}! 🙌 Glad you replied to my story. Here\'s more info:', link: '', variations: [], imageUrl: '', delaySeconds: null }],
    },
  },
  {
    id: 'dm_auto_responder',
    title: 'DM Auto Responder',
    badge: 'quick',
    badgeTone: 'brand',
    description: 'Automatically reply to direct messages with personalised responses and call-to-action buttons.',
    icon: Send,
    iconTone: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300',
    draft: {
      trigger: TRIGGER_TYPE.DM,
      condition: { type: CONDITION_TYPE.ANY, keyword: '', matchType: MATCH_TYPE.CONTAINS },
      actions: [{ type: ACTION_TYPE.SEND_MESSAGE, message: 'Hey {{username}}! 👋 Thanks for reaching out. I\'ll get back to you shortly!', link: '', variations: [], imageUrl: '', delaySeconds: null }],
    },
  },
  {
    id: 'next_post',
    title: 'Next Post Automation',
    badge: 'quick',
    badgeTone: 'brand',
    description: 'Fire on comments of your next uploaded post only — perfect for launches and drops.',
    icon: Zap,
    iconTone: 'bg-purple-100 text-purple-700 dark:bg-purple-500/15 dark:text-purple-300',
    draft: {
      trigger: TRIGGER_TYPE.NEXT_POST,
      condition: { type: CONDITION_TYPE.ANY, keyword: '', matchType: MATCH_TYPE.CONTAINS },
      actions: [{ type: ACTION_TYPE.SEND_MESSAGE, message: 'Hey {{username}}! 🎉 Thanks for commenting on my latest post!', link: '', variations: [], imageUrl: '', delaySeconds: null }],
    },
  },
];

const BADGE_STYLE = {
  brand:   'bg-brand-100 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300',
  neutral: 'bg-ink-100 text-ink-500 dark:bg-ink-800 dark:text-ink-400',
};

// ─── Modal ───────────────────────────────────────────
const TemplatePickerModal = ({ open, onClose }) => {
  const navigate = useNavigate();
  const startCreate = useBuilderStore((s) => s.startCreate);
  const startEdit   = useBuilderStore((s) => s.startEdit);

  const handleTemplate = (template) => {
    if (template.comingSoon) return;

    // Pre-fill the builder store with the template's draft
    startEdit({
      id: null,
      name: '',
      ...template.draft,
      enabled: true,
      cooldownMinutes: 0,
      publicReplyEnabled: false,
      publicReplies: [],
      followGateEnabled: false,
      followGateMessage: '',
      followGateButtonLabel: '',
      botProtectionEnabled: false,
      botProtectionMinDelaySeconds: 2,
      botProtectionMaxDelaySeconds: 8,
      targetPostMode: 'ALL',
      targetPostId: null,
    });

    onClose();
    navigate(ROUTES.AUTOMATION_NEW);
  };

  const handleScratch = () => {
    startCreate();
    onClose();
    navigate(ROUTES.AUTOMATION_NEW);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Choose a Template"
      size="lg"
    >
      <p className="mb-6 text-sm text-ink-500 dark:text-ink-400">
        Pick the type of automation you want to build — or start from scratch.
      </p>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {TEMPLATES.map((t, i) => (
          <TemplateCard
            key={t.id}
            template={t}
            index={i}
            onClick={() => handleTemplate(t)}
          />
        ))}
      </div>

      {/* Start from scratch */}
      <button
        type="button"
        onClick={handleScratch}
        className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-ink-200 px-4 py-3 text-sm font-medium text-ink-600 transition-colors hover:border-ink-300 hover:bg-ink-50 dark:border-ink-800 dark:text-ink-400 dark:hover:border-ink-700 dark:hover:bg-ink-800/40"
      >
        Start from scratch
        <ArrowRight size={14} />
      </button>
    </Modal>
  );
};

// ─── Template card ───────────────────────────────────
const TemplateCard = ({ template, index, onClick }) => {
  const Icon = template.icon;

  return (
    <motion.button
      type="button"
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2, delay: index * 0.04 }}
      onClick={onClick}
      disabled={template.comingSoon}
      className={cn(
        'group flex items-start gap-4 rounded-2xl border p-4 text-left transition-all',
        template.comingSoon
          ? 'cursor-not-allowed border-ink-100 bg-ink-50/60 opacity-60 dark:border-ink-800 dark:bg-ink-900/40'
          : 'border-ink-200 bg-white hover:border-brand-300 hover:shadow-soft dark:border-ink-800 dark:bg-ink-900 dark:hover:border-brand-500/40'
      )}
    >
      <span className={cn(
        'grid h-10 w-10 shrink-0 place-items-center rounded-xl',
        template.iconTone
      )}>
        <Icon size={18} />
      </span>

      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold text-ink-900 dark:text-ink-100">
            {template.title}
          </p>
          {template.badge && (
            <span className={cn(
              'rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide',
              BADGE_STYLE[template.badgeTone]
            )}>
              {template.badge}
            </span>
          )}
        </div>
        <p className="mt-1 text-xs leading-relaxed text-ink-500 dark:text-ink-400">
          {template.description}
        </p>
      </div>
    </motion.button>
  );
};

export default TemplatePickerModal;