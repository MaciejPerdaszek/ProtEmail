import {create} from 'zustand';
import {persist} from 'zustand/middleware';
import {Client} from '@stomp/stompjs';
import {toast} from "react-toastify";
import {MailboxConnectionService} from "../api/MailboxConnectionService.js";

const useScanningStore = create(
    persist(
        (set, get) => ({
            scannedMailboxes: {},
            stompClients: {},

            initializeWebSocket: (email, mailboxConfig) => {
                const store = get();
                const mailboxKey = `${email}_${mailboxConfig.userId}`;

                if (!store.stompClients[mailboxKey]) {
                    const client = new Client({
                        brokerURL: 'ws://localhost:8080/websocket',
                        reconnectDelay: 5000,
                        debug: (str) => console.log('STOMP debug: ' + str),
                        onConnect: () => {
                            console.log("WebSocket connected for", email, "user", mailboxConfig.userId);

                            client.subscribe(`/topic/connect/${email}/${mailboxConfig.userId}`, (message) => {
                                try {
                                    const response = JSON.parse(message.body);
                                    console.log("Received message:", response);

                                    if (response.error === "SUCCESS") {
                                        toast(`Successfully connected to ${email}`, {type: 'success'});
                                        set((state) => ({
                                            scannedMailboxes: {
                                                ...state.scannedMailboxes,
                                                [mailboxKey]: {
                                                    ...(state.scannedMailboxes[mailboxKey] || {}),
                                                    isScanning: true,
                                                    lastScan: new Date().toISOString(),
                                                    emailsScanned: state.scannedMailboxes[mailboxKey]?.emailsScanned || 0,
                                                    threatsFound: state.scannedMailboxes[mailboxKey]?.threatsFound || 0
                                                }
                                            }
                                        }));
                                    } else if (response.error === "ERROR") {
                                        toast(`Failed to connect to ${email}, ${response.cause}`, {type: 'error'});
                                        store.disconnectMailbox(email, mailboxConfig.userId);
                                    }
                                } catch (error) {
                                    console.error("Error parsing message:", error);
                                }
                            });

                            client.subscribe(`/topic/emails/${email}/${mailboxConfig.userId}`, (message) => {
                                try {
                                    const threatData = JSON.parse(message.body);
                                    console.log("Received threat data:", threatData);

                                    set((state) => ({
                                        scannedMailboxes: {
                                            ...state.scannedMailboxes,
                                            [mailboxKey]: {
                                                ...(state.scannedMailboxes[mailboxKey] || {}),
                                                emailsScanned: (state.scannedMailboxes[mailboxKey]?.emailsScanned || 0) + 1,
                                                threatsFound: (threatData.threatLevel === "High" || threatData.threatLevel === "Medium" || threatData.threatLevel === "Low"
                                                    ? (state.scannedMailboxes[mailboxKey]?.threatsFound || 0) + 1
                                                    : state.scannedMailboxes[mailboxKey]?.threatsFound || 0)
                                            }
                                        }
                                    }));

                                    if (threatData.threatLevel === "High" || threatData.threatLevel === "Medium" || threatData.threatLevel === "Low")
                                        toast.warning(`Threat detected in ${email}`);

                                } catch (error) {
                                    console.error("Error processing threat data:", error);
                                }
                            });

                            client.publish({
                                destination: '/api/connect',
                                body: JSON.stringify(mailboxConfig)
                            });

                            set((state) => ({
                                scannedMailboxes: {
                                    ...state.scannedMailboxes,
                                    [mailboxKey]: {
                                        ...(state.scannedMailboxes[mailboxKey] || {}),
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
                                    [mailboxKey]: {
                                        ...(state.scannedMailboxes[mailboxKey] || {}),
                                        isScanning: false
                                    }
                                }
                            }));
                        }
                    });

                    client.activate();
                    store.setStompClient(mailboxKey, client);
                }
            },

            setStompClient: (mailboxKey, client) => {
                console.log('Setting STOMP client for', mailboxKey);
                set((state) => ({
                    stompClients: {
                        ...state.stompClients,
                        [mailboxKey]: client
                    }
                }));
            },

            getStompClient: (email, userId) => {
                const mailboxKey = `${email}_${userId}`;
                return get().stompClients[mailboxKey];
            },

            removeStompClient: (email, userId) => {
                const mailboxKey = `${email}_${userId}`;
                set((state) => {
                    const { [mailboxKey]: removed, ...rest } = state.stompClients;
                    return { stompClients: rest };
                });
            },

            disconnectMailbox: (email, userId) => {
                const mailboxKey = `${email}_${userId}`;
                const client = get().stompClients[mailboxKey];
                if (client) {
                    if (client.connected) {
                        client.publish({
                            destination: '/api/disconnect',
                            body: JSON.stringify({
                                email: email,
                                userId: userId
                            })
                        });
                        client.deactivate();
                    }

                    set((state) => {
                        const { [mailboxKey]: removed, ...rest } = state.stompClients;
                        return {
                            stompClients: rest,
                            scannedMailboxes: {
                                ...state.scannedMailboxes,
                                [mailboxKey]: {
                                    ...(state.scannedMailboxes[mailboxKey] || {}),
                                    isScanning: false
                                }
                            }
                        };
                    });
                }
            },

            disconnectAllMailboxes: (userId) => {
                const clients = get().stompClients;
                Object.keys(clients).forEach((mailboxKey) => {
                    if (mailboxKey.endsWith(`_${userId}`)) {
                        const email = mailboxKey.split('_')[0];
                        const client = clients[mailboxKey];
                        if (client.connected) {
                            client.publish({
                                destination: '/api/disconnect',
                                body: JSON.stringify({
                                    email: email,
                                    userId: userId
                                })
                            });
                            client.deactivate();
                        }
                    }
                });

                set((state) => ({
                    stompClients: Object.keys(state.stompClients).reduce((acc, key) => {
                        if (!key.endsWith(`_${userId}`)) {
                            acc[key] = state.stompClients[key];
                        }
                        return acc;
                    }, {}),
                    scannedMailboxes: Object.keys(state.scannedMailboxes).reduce((acc, key) => {
                        if (!key.endsWith(`_${userId}`)) {
                            acc[key] = state.scannedMailboxes[key];
                        } else {
                            acc[key] = {
                                ...state.scannedMailboxes[key],
                                isScanning: false
                            };
                        }
                        return acc;
                    }, {})
                }));
            },

            synchronizeState: async (user) => {
                try {
                    const serverStates = await MailboxConnectionService.fetchMailboxConnections(user.sub);

                    set((state) => ({
                        scannedMailboxes: Object.keys(serverStates).reduce((acc, email) => {
                            const mailboxKey = `${email}_${user.sub}`;
                            return {
                                ...acc,
                                [mailboxKey]: {
                                    ...(state.scannedMailboxes[mailboxKey] || {}),
                                    isScanning: serverStates[email]
                                }
                            };
                        }, state.scannedMailboxes)
                    }));
                } catch (error) {
                    console.error('Failed to synchronize states:', error);
                }
            }
        }),
        {
            name: 'mailbox-scanning-storage',
            partialize: (state) => ({
                scannedMailboxes: Object.keys(state.scannedMailboxes).reduce((acc, key) => ({
                    ...acc,
                    [key]: {
                        isScanning: state.scannedMailboxes[key].isScanning,
                        lastScan: state.scannedMailboxes[key].lastScan,
                        emailsScanned: state.scannedMailboxes[key].emailsScanned,
                        threatsFound: state.scannedMailboxes[key].threatsFound
                    }
                }), {})
            })
        }
    )
);

export { useScanningStore };