import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

/**
 * Failed-jobs REST client. Mirrors the contactService / logService
 * shape — thin axios wrapper, one method per endpoint, returns the
 * unwrapped data from the {@code ApiResponse} envelope.
 */
const failedJobService = {
  list: async () => {
    const { data } = await api.get(API_ENDPOINTS.FAILED_JOBS);
    return data; // → FailedJobResponse[]
  },

  retry: async (id) => {
    const { data } = await api.post(API_ENDPOINTS.FAILED_JOB_RETRY(id));
    return data; // → { jobId }
  },

  delete: async (id) => {
    await api.delete(API_ENDPOINTS.FAILED_JOB_BY_ID(id));
  },
};

export default failedJobService;
