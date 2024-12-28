import React, { useState } from 'react';
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { Container, Button } from 'reactstrap';
import { Mail, Clock, AlertTriangle, Undo } from 'lucide-react';
import { Client } from "@stomp/stompjs";
import { useScanningStore } from '../store/scannigStore.js';
import '../stylings/Mailbox.css';

export function Mailbox() {
    const { email } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { mailbox = {}} = location.state || {};
    const [error, setError] = useState(null);

    const {scannedMailboxes, setMailboxScanning, updateThreatsFound, setStompClient, getStompClient, disconnectMailbox} = useScanningStore();

    const currentMailboxState = scannedMailboxes[email] || {
        isScanning: false,
        lastScan: null,
        threatsFound: 0
    };

    const handleStartScan = async () => {
        // Check if there's already an active client
        const existingClient = getStompClient(email);
        if (existingClient && existingClient.connected) {
            console.log("Using existing connection");
            setMailboxScanning(email, true);
            return;
        }

        try {
            const client = new Client({
                brokerURL: 'ws://localhost:8080/gs-guide-websocket',
                reconnectDelay: 5000,
                debug: (str) => console.log('STOMP debug: ' + str),
                onConnect: () => {
                    console.log("WebSocket connected");
                    client.subscribe(`/topic/${email}`, (message) => {
                        const data = JSON.parse(message.body);
                        if (data.threatsFound !== undefined) {
                            updateThreatsFound(email, data.threatsFound);
                        }
                        console.log("Received message:", data);
                    });

                    const emailConfig = {
                        protocol: "imap",
                        host: mailbox.type,
                        port: "993",
                        username: email,
                    };

                    client.publish({
                        destination: '/api/connect',
                        body: JSON.stringify(emailConfig)
                    });

                    setMailboxScanning(email, true);
                    setError(null);
                },
                onWebSocketError: (error) => {
                    console.error('WebSocket Error:', error);
                    setError('WebSocket connection error');
                    disconnectMailbox(email);
                },
                onStompError: (frame) => {
                    console.error('STOMP Error:', frame.headers['message']);
                    setError('STOMP protocol error');
                    disconnectMailbox(email);
                },
                onDisconnect: () => {
                    console.log('STOMP client disconnected');
                    setMailboxScanning(email, false);
                }
            });

            client.activate();
            setStompClient(email, client);
        } catch (error) {
            console.error('Error during connection setup:', error);
            setError('Failed to setup connection');
            disconnectMailbox(email);
        }
    };

    const handleNavigateBack = () => {
        navigate('/dashboard');
    };

    return (
        <Container className="mailbox-detail-container">
            <Container className="mailbox-detail-header">
                <Container className="mailbox-detail-title">
                    <Mail className="mailbox-icon"/>
                    <h2>{email}</h2>
                </Container>
                <Button className="close-button" onClick={handleNavigateBack}>
                    <Undo/>
                </Button>
            </Container>

            <div className="mailbox-detail-content">
                <div className="mailbox-detail-card">
                    <h3 className="detail-section-title">
                        <AlertTriangle className="section-icon"/>
                        Scan Status
                    </h3>
                    <div className="status-container">
                        <div className="status-item">
                            <Clock className="status-icon"/>
                            <div className="status-info">
                                <span className="status-label">Last Scan:</span>
                                <span>
                                    {currentMailboxState.lastScan
                                        ? new Date(currentMailboxState.lastScan).toLocaleString()
                                        : 'Never'}
                                </span>
                            </div>
                        </div>

                        <div className="status-item">
                            <AlertTriangle className="status-icon"/>
                            <div className="status-info">
                                <span className="status-label">Threats Found:</span>
                                <span>{currentMailboxState.threatsFound}</span>
                            </div>
                        </div>
                    </div>

                    {error && (
                        <div className="error-message" style={{ color: 'red', marginBottom: '10px' }}>
                            {error}
                        </div>
                    )}

                    {currentMailboxState.isScanning ? (
                        <Button
                            className="scan-button"
                            onClick={() => disconnectMailbox(email)}
                        >
                            Stop Scan
                        </Button>
                    ) : (
                        <Button
                            className="scan-button"
                            onClick={handleStartScan}
                        >
                            Start Scan
                        </Button>
                    )}
                </div>
            </div>
        </Container>
    );
}