import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { Client } from '@stomp/stompjs';

const useScanningStore = create(
    persist(
        (set, get) => ({
            scannedMailboxes: {},
            stompClients: {},

            initializeWebSocket: (email, mailboxConfig) => {
                const store = get();
                if (!store.stompClients[email]) {
                    const client = new Client({
                        brokerURL: 'ws://localhost:8080/gs-guide-websocket',
                        reconnectDelay: 5000,
                        debug: (str) => console.log('STOMP debug: ' + str),
                        onConnect: () => {
                            console.log("WebSocket connected for", email);

                            client.subscribe(`/user/${email}/topic/${email}`, (message) => {
                                const data = message.body;
                                console.log("Received message:", data);

                                if (data.includes('New email received')) {
                                    set((state) => ({
                                        scannedMailboxes: {
                                            ...state.scannedMailboxes,
                                            [email]: {
                                                ...(state.scannedMailboxes[email] || {}),
                                                isScanning: true,
                                                lastScan: new Date().toISOString(),
                                                threatsFound: (state.scannedMailboxes[email]?.threatsFound || 0) + 1
                                            }
                                        }
                                    }));
                                }
                            });
                            
                            client.publish({
                                destination: '/api/connect',
                                body: JSON.stringify({
                                    ...mailboxConfig,
                                    sessionId: client.connectionId
                                })
                            });

                            set((state) => ({
                                scannedMailboxes: {
                                    ...state.scannedMailboxes,
                                    [email]: {
                                        ...(state.scannedMailboxes[email] || {}),
                                        isScanning: true,
                                        lastScan: new Date().toISOString()
                                    }
                                }
                            }));
                        },
                        onDisconnect: () => {
                            console.log('WebSocket disconnected for', email);
                            set((state) => ({
                                scannedMailboxes: {
                                    ...state.scannedMailboxes,
                                    [email]: {
                                        ...(state.scannedMailboxes[email] || {}),
                                        isScanning: false
                                    }
                                }
                            }));
                        }
                    });

                    client.activate();
                    store.setStompClient(email, client);
                }
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

            synchronizeState: async () => {
                try {
                    const response = await fetch('/api/emails/connection-states');
                    const serverStates = await response.json();

                    set((state) => ({
                        scannedMailboxes: Object.keys(serverStates).reduce((acc, email) => ({
                            ...acc,
                            [email]: {
                                ...(state.scannedMailboxes[email] || {}),
                                isScanning: serverStates[email]
                            }
                        }), state.scannedMailboxes)
                    }));
                } catch (error) {
                    console.error('Failed to synchronize states:', error);
                }
            }
        }),
        {
            name: 'mailbox-scanning-storage',
            partialize: (state) => ({
                scannedMailboxes: Object.keys(state.scannedMailboxes).reduce((acc, email) => ({
                    ...acc,
                    [email]: {
                        isScanning: state.scannedMailboxes[email].isScanning,
                        lastScan: state.scannedMailboxes[email].lastScan,
                        threatsFound: state.scannedMailboxes[email].threatsFound
                    }
                }), {})
            })
        }
    )
);

export { useScanningStore };