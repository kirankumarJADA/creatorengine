import api from './api.js';

/**
 * Uploads a DM image file to the backend (which stores it in Firebase
 * Storage and returns a public URL). Used by ActionStep's "Send Image"
 * picker for the SEND_MESSAGE / SEND_LINK action's optional image.
 */
const mediaService = {
  uploadDmImage: async (file) => {
    const formData = new FormData();
    formData.append('file', file);

    const { data } = await api.post('/media/dm-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data; // → { imageUrl }
  },
};

export default mediaService;