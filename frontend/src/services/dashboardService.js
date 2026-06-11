import logService from './logService.js';
import contactService from './contactService.js';
import instagramService from './instagramService.js';

/**
 * Aggregates the data the dashboard needs in one parallel fetch.
 * Each leg is independently catchable — a single failing endpoint
 * (e.g. IG not connected yet → status returns NOT_CONNECTED, not a
 * thrown error) shouldn't blank the whole page.
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