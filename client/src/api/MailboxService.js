import axiosInstance from '../axiosConfig';

export const MailboxService = {
    fetchMailboxes: async (userId) => {
        const response = await axiosInstance.get(`/mailboxes/${userId}`);
        return response.data;
    },

    addMailbox: async (userId, mailboxData) => {
        await axiosInstance.post(`/mailboxes/${userId}`, mailboxData);
    },

    updateMailbox: async (userId, mailboxId, mailboxData) => {
        await axiosInstance.put(`/mailboxes/${userId}/${mailboxId}`, mailboxData);
    },

    deleteMailbox: async (userId, mailboxId) => {
        await axiosInstance.delete(`/mailboxes/${userId}/${mailboxId}`);
    }
};