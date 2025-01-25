import axiosInstance from '../axiosConfig';

export const MailboxConnectionService = {

    fetchMailboxConnections: async (userId) => {
        const response = await axiosInstance.get(`/mailbox-connections/${userId}`);
        return response.data;
    },
}
