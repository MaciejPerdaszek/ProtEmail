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

                            client.subscribe(`/topic/connect/${email}`, (message) => {
                                try {
                                    const response = JSON.parse(message.body);
                                    console.log("Received message:", response);

                                    if (response.error === "SUCCESS") {
                                        toast(`Successfully connected to ${email}`, {type: 'success'});
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
                                    } else if (response.error === "ERROR") {
                                        toast(`Failed to connect to ${email}`, {type: 'error'});
                                        set((state) => ({
                                            scannedMailboxes: {
                                                ...state.scannedMailboxes,
                                                [email]: {
                                                    ...(state.scannedMailboxes[email] || {}),
                                                    isScanning: false
                                                }
                                            }
                                        }));
                                        client.deactivate();
                                        store.removeStompClient(email);
                                    }
                                } catch (error) {
                                    console.error("Error parsing message:", error);
                                }
                            });

                            client.publish({
                                destination: '/api/connect',
                                body: JSON.stringify(mailboxConfig)
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
                            toast (`Disconnected from ${email}`, {type: 'info'});
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
                console.log('Setting STOMP client for', email);
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

            removeStompClient: (email) => {
                set((state) => {
                    const { [email]: removed, ...rest } = state.stompClients;
                    return { stompClients: rest };
                });
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

            disconnectAllMailboxes: () => {
                const clients = get().stompClients;
                Object.keys(clients).forEach((email) => {
                    const client = clients[email];
                    if (client.connected) {
                        client.publish({
                            destination: '/api/disconnect',
                            body: email
                        });
                        client.deactivate();
                    }
                });

                set({
                    stompClients: {},
                    scannedMailboxes: Object.keys(get().scannedMailboxes).reduce((acc, email) => ({
                        ...acc,
                        [email]: {
                            ...(get().scannedMailboxes[email] || {}),
                            isScanning: false
                        }
                    }), {})
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