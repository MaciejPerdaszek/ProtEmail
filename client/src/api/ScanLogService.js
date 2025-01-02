import axiosInstance from '../axiosConfig';

export const ScanLogService = {
    fetchScanLogs: async (mailboxId = null) => {
        try {
            const url = mailboxId ?
                `/scan-logs?mailboxId=${mailboxId}` :
                '/scan-logs';

            const response = await axiosInstance.get(url);
            return response.data;
        } catch (error) {
            console.error('Error fetching scan logs:', error);
            throw error;
        }
    },

    fetchMailboxScanLogs: async (mailboxId) => {
        const response = await axiosInstance.get(`/scan-logs/?mailbox=${mailboxId}`);
        return response.data;
    }
}