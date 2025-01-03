import axiosInstance from '../axiosConfig';

export const ScanLogService = {
    fetchScanLogs: async (mailboxIds = [], page = 0, size = 5) => {
        try {
            const params = new URLSearchParams({
                page: page.toString(),
                size: size.toString()
            });

            if (mailboxIds && mailboxIds.length > 0) {
                mailboxIds.forEach(id => params.append('mailboxId', id.toString()));
            }

            const url = `/scan-logs?${params.toString()}`;
            const response = await axiosInstance.get(url);

            return {
                data: response.data.content,
                total: response.data.totalElements,
                totalPages: response.data.totalPages,
                currentPage: response.data.number
            };
        } catch (error) {
            console.error('Error fetching scan logs:', error);
            throw error;
        }
    }
}