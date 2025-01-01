import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { Client } from '@stomp/stompjs';
import {toast} from "react-toastify";

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

                            client.subscribe(`/user/${email}/topic/response`, (message) => {
                                const data = JSON.parse(message.body);
                                console.log("Received message:", data);
                                toast(data.message, { type: data.error === 'ERROR' ? 'error' : 'info' });

                                if (data.error === 'ERROR') {
                                    toast(data.message, { type: 'error' });
                                    client.deactivate();
                                    store.disconnectMailbox(email);
                                } else if (data.error === 'SUCCESS') {
                                    toast(data.message, { type: 'success' });
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
                                }

                                if (data.message?.includes('New email received')) {
                                    set((state) => ({
                                        scannedMailboxes: {
                                            ...state.scannedMailboxes,
                                            [email]: {
                                                ...(state.scannedMailboxes[email] || {}),
                                                lastScan: new Date().toISOString(),
                                                threatsFound: (state.scannedMailboxes[email]?.threatsFound || 0) + 1
                                            }
                                        }
                                    }));
                                }
                            });

                            client.publish({
                                destination: '/api/connect',
                                body: JSON.stringify(mailboxConfig)
                            });
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
                toast(`Disconnecting mailbox ${email}`, { type: 'warning' });
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

            disconnectAllMailboxes: () => {
                const store = get();
                Object.keys(store.stompClients).forEach((email) => {
                    store.disconnectMailbox(email);
                });
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