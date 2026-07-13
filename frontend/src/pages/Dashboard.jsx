import logService from './logService.js';
import contactService from './contactService.js';
import instagramService from './instagramService.js';

/**
 * Aggregates the data the dashboard needs in one parallel fetch.
 * Each leg is independently catchable so a single failing endpoint
 * doesn't blank the whole page.
 *
 * The active Instagram account is already encoded in the
 * X-IG-Account-Id header (set by api.js interceptor), so all three
 * calls are automatically scoped to the selected account.
 */
const dashboardService = {
  loadAll: async () => {
    const [logsResult, contactsResult, igStatusResult] = await Promise.allSettled([
      logService.list(),
      contactService.list(),
      instagramService.getStatus(),
    ]);

    return {
      logs:     logsResult.status     === 'fulfilled' ? logsResult.value     : [],
      contacts: contactsResult.status === 'fulfilled' ? contactsResult.value : [],
      igStatus: igStatusResult.status === 'fulfilled' ? igStatusResult.value : null,
    };
  },
};

export default dashboardService;