import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

/**
 * Contacts REST client.
 *
 * The backend returns the full list scoped to the current user;
 * pagination, search, and source filtering happen in the page itself
 * (see {@code pages/Contacts.jsx}). Filtering server-side is the
 * future-scaling story but isn't worth the Firestore index work
 * until per-user contact counts grow past a few thousand.
 */
const contactService = {
  list: async () => {
    const { data } = await api.get(API_ENDPOINTS.CONTACTS);
    return data; // → ContactResponse[]
  },

  /**
   * Download all contacts as a CSV file.
   * Returns a Blob the caller can turn into a download link.
   */
  exportCsv: async () => {
    const response = await api.get(`${API_ENDPOINTS.CONTACTS}/export`, {
      responseType: 'blob',
    });
    return response.data; // → Blob (text/csv)
  },
};

export default contactService;
