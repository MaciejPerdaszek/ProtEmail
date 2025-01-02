import axiosInstance from '../axiosConfig';

export const ScanLogService = {
    fetchScanLogs: async (mailboxId = null, page = 0, pageSize = pageSize) => {
        try {
            let url = '/scan-logs';
            const params = new URLSearchParams({
                page: page,
                size: pageSize
            });

            if (mailboxId) {
                params.append('mailboxId', mailboxId);
            }

            url = `${url}?${params.toString()}`;

            const [logsResponse, countResponse] = await Promise.all([
                axiosInstance.get(url),
                axiosInstance.get(`/scan-logs/count${mailboxId ? `?mailboxId=${mailboxId}` : ''}`)
            ]);

            return {
                data: logsResponse.data,
                total: countResponse.data,
                totalPages: Math.ceil(countResponse.data / pageSize),
                currentPage: page
            };
        } catch (error) {
            console.error('Error fetching scan logs:', error);
            throw error;
        }
    }

}