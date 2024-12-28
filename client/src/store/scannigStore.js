import { create } from 'zustand';

const useScanningStore = create((set, get) => ({
    scannedMailboxes: {},
    stompClients: {},
    stompConfig: null,

    setStompConfig: (config) => set({ stompConfig: config }),

    setMailboxScanning: (email, isScanning) => {
        set((state) => ({
            scannedMailboxes: {
                ...state.scannedMailboxes,
                [email]: {
                    ...(state.scannedMailboxes[email] || {}),
                    isScanning,
                    lastScan: isScanning ? new Date().toISOString() : state.scannedMailboxes[email]?.lastScan
                }
            }
        }));
    },

    updateThreatsFound: (email, threatsFound) => {
        set((state) => ({
            scannedMailboxes: {
                ...state.scannedMailboxes,
                [email]: {
                    ...(state.scannedMailboxes[email] || {}),
                    threatsFound
                }
            }
        }));
    },

    setStompClient: (email, client) => {
        set((state) => ({
            stompClients: {
                ...state.stompClients,
                [email]: client
            }
        }));
    },

    getStompClient: (email) => {
        return get().stompClients[email];
    },

    disconnectMailbox: (email) => {
        const client = get().stompClients[email];
        if (client) {
            if (client.connected) {
                client.publish({
                    destination: '/api/disconnect',
                    body: email
                });
                client.deactivate();
            }
            set((state) => {
                const { [email]: removed, ...rest } = state.stompClients;
                return {
                    stompClients: rest,
                    scannedMailboxes: {
                        ...state.scannedMailboxes,
                        [email]: {
                            ...(state.scannedMailboxes[email] || {}),
                            isScanning: false
                        }
                    }
                };
            });
        }
    },

    resetMailbox: (email) => {
        set((state) => {
            const { [email]: removed, ...rest } = state.scannedMailboxes;
            return { scannedMailboxes: rest };
        });
    }
}));

export { useScanningStore };