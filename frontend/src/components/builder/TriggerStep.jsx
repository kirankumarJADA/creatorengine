import { MessageSquare, Send, AtSign, Sparkles } from 'lucide-react';
import RadioCardGroup from '../ui/RadioCardGroup.jsx';
import { useBuilderStore } from '../../store/builderStore.js';
import { TRIGGER_TYPE } from '../../utils/constants.js';

const TRIGGER_OPTIONS = [
  {
    value: TRIGGER_TYPE.COMMENT,
    label: 'Comment on Post/Reel',
    description: 'Fire when someone comments on a post or reel.',
    icon: MessageSquare,
    tone: 'brand',
  },
  {
    value: TRIGGER_TYPE.DM,
    label: 'DM Message',
    description: 'Fire when someone sends you a direct message.',
    icon: Send,
    tone: 'success',
  },
  {
    value: TRIGGER_TYPE.STORY_REPLY,
    label: 'Story Reply',
    description: 'Fire when someone replies to one of your stories.',
    icon: AtSign,
    tone: 'warning',
  },
  {
    value: TRIGGER_TYPE.NEXT_POST,
    label: 'Next Post',
    description: 'Fire on comments of your NEXT uploaded post only.',
    icon: Sparkles,
    tone: 'brand',
  },
];

const TriggerStep = () => {
  const trigger    = useBuilderStore((s) => s.draft.trigger);
  const setTrigger = useBuilderStore((s) => s.setTrigger);

  return (
    <div>
      <header className="mb-6">
        <h2 className="text-xl font-semibold text-ink-900 dark:text-ink-100">
          What should kick this off?
        </h2>
        <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
          Pick the Instagram event that triggers the automation.
        </p>
      </header>

      <RadioCardGroup
        name="trigger"
        options={TRIGGER_OPTIONS}
        value={trigger}
        onChange={setTrigger}
        columns={2}
      />
    </div>
  );
};

export default TriggerStep;