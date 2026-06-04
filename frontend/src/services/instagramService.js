import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

/**
 * Instagram connection REST client.
 *
 * Note that {@code disconnect} uses POST (not DELETE) to match the
 * backend's {@code POST /api/instagram/disconnect} contract — Meta's
 * OAuth flow is a state transition more than a resource deletion.
 */
const instagramService = {
  /** Returns { authUrl: string } — the SPA then does window.location.href = authUrl. */
  startConnect: async () => {
    const { data } = await api.get(API_ENDPOINTS.IG_CONNECT);
    return data; // { authUrl }
  },

  /**
   * Returns the current connection status payload:
   *   { status, instagramUserId?, username?, name?, pageId?, profilePictureUrl?,
   *     connectedAt?, lastSyncAt?, tokenExpiresAt? }
   * `status` is one of CONNECTION_STATUS values.
   */
  getStatus: async () => {
    // The status endpoint shouldn't toast on 401 since we may call it
    // before the user is fully authenticated — the axios interceptor
    // already handles auth failures by redirecting.
    const { data } = await api.get(API_ENDPOINTS.IG_STATUS);
    return data;
  },

  /**
   * Lists the connected account's recent posts for the automation
   * post-picker. Returns an array of media items:
   *   { id, caption, mediaType, mediaUrl, thumbnailUrl, permalink, timestamp }
   * (some fields may arrive snake_case from the backend; the picker
   * handles both.)
   */
  getMedia: async () => {
    const { data } = await api.get('/instagram/media');
    return Array.isArray(data) ? data : [];
  },

  disconnect: async () => {
    await api.post(API_ENDPOINTS.IG_DISCONNECT);
  },
};

export default instagramService;