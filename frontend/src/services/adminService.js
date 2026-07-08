import api from './api.js';
import { API_ENDPOINTS } from '../utils/constants.js';

const adminService = {
  getDashboard: async () => {
    const { data } = await api.get(API_ENDPOINTS.ADMIN_DASHBOARD);
    return data;
  },

  listUsers: async () => {
    const { data } = await api.get(API_ENDPOINTS.ADMIN_USERS);
    return data;
  },

  enableUser: async (uid) => {
    const { data } = await api.patch(API_ENDPOINTS.ADMIN_USER_ENABLE(uid));
    return data;
  },

  disableUser: async (uid) => {
    const { data } = await api.patch(API_ENDPOINTS.ADMIN_USER_DISABLE(uid));
    return data;
  },

  deleteUser: async (uid) => {
    await api.delete(API_ENDPOINTS.ADMIN_USER_BY_ID(uid));
  },

  listAutomations: async () => {
    const { data } = await api.get(API_ENDPOINTS.ADMIN_AUTOMATIONS);
    return data;
  },

  toggleAutomation: async (uid, id) => {
    const { data } = await api.patch(API_ENDPOINTS.ADMIN_AUTOMATION_TOGGLE(uid, id));
    return data;
  },

  deleteAutomation: async (uid, id) => {
    await api.delete(API_ENDPOINTS.ADMIN_AUTOMATION_BY_ID(uid, id));
  },

  listLogs: async (limit) => {
    const { data } = await api.get(API_ENDPOINTS.ADMIN_LOGS, { params: { limit } });
    return data;
  },

  listFailedJobs: async (limit) => {
    const { data } = await api.get(API_ENDPOINTS.ADMIN_FAILED_JOBS, { params: { limit } });
    return data;
  },

  retryFailedJob: async (uid, id) => {
    const { data } = await api.post(API_ENDPOINTS.ADMIN_FAILED_JOB_RETRY(uid, id));
    return data;
  },

  deleteFailedJob: async (uid, id) => {
    await api.delete(API_ENDPOINTS.ADMIN_FAILED_JOB_BY_ID(uid, id));
  },

  getSystemStatus: async () => {
    const { data } = await api.get(API_ENDPOINTS.ADMIN_SYSTEM);
    return data;
  },
};

export default adminService;