import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

/**
 * AI message-assistant client. Single call: POST a brief
 * (goal/tone/audience/cta) and get back exactly 3 DM suggestions.
 *
 * The backend never errors on AI failures — it falls back to template
 * suggestions transparently. So callers don't need a separate error
 * path beyond standard network/auth failures.
 */
const aiMessageService = {
  generate: async ({ goal, tone, audience, cta }) => {
    const { data } = await api.post(API_ENDPOINTS.AI_GENERATE_MESSAGE, {
      goal,
      tone,
      audience,
      cta: cta || null,
    });
    return data; // → { suggestions: string[], provider: 'openai' | 'fallback' }
  },
};

export default aiMessageService;
