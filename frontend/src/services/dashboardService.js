import logService from './logService.js';
import contactService from './contactService.js';
import instagramService from './instagramService.js';

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