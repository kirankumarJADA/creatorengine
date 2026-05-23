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
};

export default contactService;
