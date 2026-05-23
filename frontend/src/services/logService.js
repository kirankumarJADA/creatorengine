import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

/**
 * Activity-logs REST client.
 *
 * The backend returns up to 500 most-recent rows for the current
 * user; pagination, search, status / automation / date filters all
 * happen in the page itself (see {@code pages/ActivityLogs.jsx}),
 * matching the Contacts page pattern.
 */
const logService = {
  list: async () => {
    const { data } = await api.get(API_ENDPOINTS.LOGS);
    return data; // → LogResponse[]
  },
};

export default logService;
