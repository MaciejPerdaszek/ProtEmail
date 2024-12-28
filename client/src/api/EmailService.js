import axiosInstance from '../axiosConfig';

export const EmailService = {

    fetchEmails: async (imapData) => {
        const response = await axiosInstance.post('/emails/fetch', imapData);
        return response.data;
    },

    getMonitoredMailboxes: async (userId) => {
        const response = await axiosInstance.get(`/emails/monitored-emails`);
        return response.data;
    }
}