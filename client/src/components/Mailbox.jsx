import React, { useState, useEffect } from 'react';
import '../stylings/Mailbox.css';
import { Container, Form, FormGroup, Label, Input, Button } from 'reactstrap';
import { Mail, Search, Shield, Clock, AlertTriangle, Undo } from 'lucide-react';
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { EmailService } from "../api/EmailService.js";
import { Client } from "@stomp/stompjs";

export function Mailbox() {
    const { email } = useParams();
    const location = useLocation();
    const mailbox = location.state;
    const navigate = useNavigate();

    const [scanSettings, setScanSettings] = useState({
        scanDepth: 5,
        autoScan: false,
        scanFrequency: 'daily',
        scanType: 'full'
    });

    const [scanStatus, setScanStatus] = useState({
        isScanning: false,
        lastScan: null,
        threatsFound: 0
    });

    const [stompClient, setStompClient] = useState(null);
    const [isConnected, setIsConnected] = useState(false);

    const handleScanSettingsChange = (e) => {
        const { name, value, type, checked } = e.target;
        setScanSettings(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleStartScan = async () => {
        if (!isConnected) {
            const client = new Client({
                brokerURL: 'http://localhost:8080/gs-guide-websocket',
                reconnectDelay: 5000,
                debug: (str) => console.log('STOMP debug: ' + str),
                onConnect: () => {
                    console.log("WebSocket connected");
                    client.subscribe('/topic/emails', (message) => {
                        const body = JSON.parse(message.body);
                        console.log("Received message:", body);
                        // setScanStatus(prev => ({
                        //     ...prev,
                        //     isScanning: false,
                        //     lastScan: body.lastScan || new Date().toLocaleString(),
                        //     threatsFound: body.threatsFound || 0
                        // }));
                    });
                },
                onWebSocketError: (error) => {
                    console.error('WebSocket Error:', error);
                    setScanStatus(prev => ({ ...prev, isScanning: false }));
                },
                onStompError: (frame) => {
                    console.error('STOMP Error:', frame.headers['message']);
                },
            });

            client.activate();
            setStompClient(client);
            setIsConnected(true);
        } else {
            console.log("WebSocket is already connected.");
        }

        if (stompClient && stompClient.connected) {
            stompClient.publish({
                destination: '/api/emails',
                body: JSON.stringify({ message: 'Start scan' })
            });

            console.log("Scan started");
        } else {
            console.error("WebSocket is not connected");
        }
    };

    const handleCloseConnection = () => {
        if (stompClient) {
            stompClient.deactivate(); // Dezaktywacja połączenia WebSocket
            setIsConnected(false); // zmiana stanu połączenia na false
            console.log("WebSocket connection closed.");
        }
    };

    const handleGetEmail = async () => {
        try {
            const imapData = {
                host: mailbox.type,
                username: mailbox.email,
                messageCount: scanSettings.scanDepth
            };
            await EmailService.fetchEmails(imapData);
        } catch (error) {
            console.error('Failed to fetch emails:', error);
        }
    };

    // useEffect(() => {
    // }, []);

    return (
        <Container className="mailbox-detail-container">
            <Container className="mailbox-detail-header">
                <Container className="mailbox-detail-title">
                    <Mail className="mailbox-icon"/>
                    <h2>{email}</h2>
                </Container>
                <Button className="close-button" onClick={() => navigate('/dashboard')}>
                    <Undo/>
                </Button>
            </Container>

            <div className="mailbox-detail-content">
                <div className="mailbox-detail-card">
                    <h3 className="detail-section-title">
                        <Search className="section-icon"/>
                        Scan Settings
                    </h3>
                    <Form>
                        <FormGroup>
                            <Label className="detail-label">Number of emails to scan</Label>
                            <Input
                                type="number"
                                name="scanDepth"
                                value={scanSettings.scanDepth}
                                onChange={handleScanSettingsChange}
                                className="detail-input"
                                min="1"
                                max="1000"
                            />
                        </FormGroup>

                        <FormGroup>
                            <Label className="detail-label">Scan Type</Label>
                            <Input
                                type="select"
                                name="scanType"
                                value={scanSettings.scanType}
                                onChange={handleScanSettingsChange}
                                className="detail-input"
                            >
                                <option value="full">Full Scan</option>
                                <option value="quick">Quick Scan</option>
                                <option value="custom">Custom Scan</option>
                            </Input>
                        </FormGroup>

                        <FormGroup>
                            <Label className="detail-label">Scan Frequency</Label>
                            <Input
                                type="select"
                                name="scanFrequency"
                                value={scanSettings.scanFrequency}
                                onChange={handleScanSettingsChange}
                                className="detail-input"
                            >
                                <option value="daily">Daily</option>
                                <option value="weekly">Weekly</option>
                                <option value="monthly">Monthly</option>
                            </Input>
                        </FormGroup>

                        <FormGroup className="checkbox-group">
                            <Input
                                type="checkbox"
                                name="autoScan"
                                checked={scanSettings.autoScan}
                                onChange={handleScanSettingsChange}
                                className="detail-checkbox"
                            />
                            <Label className="checkbox-label">
                                <Shield className="checkbox-icon"/>
                                Enable Auto-Scan
                            </Label>
                        </FormGroup>
                    </Form>
                </div>

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
                                <span>{scanStatus.lastScan || 'Never'}</span>
                            </div>
                        </div>

                        <div className="status-item">
                            <AlertTriangle className="status-icon"/>
                            <div className="status-info">
                                <span className="status-label">Threats Found:</span>
                                <span>{scanStatus.threatsFound}</span>
                            </div>
                        </div>
                    </div>

                    {isConnected ? (
                        <Button
                            className="scan-button"
                            onClick={handleCloseConnection}
                        >
                            Close Connection
                        </Button>
                    ) : (
                        <Button
                            className="scan-button"
                            onClick={handleStartScan}
                        >
                            Start Scan
                        </Button>
                    )}
                    {/*<Button*/}
                    {/*    className={`scan-button ${scanStatus.isScanning ? 'scanning' : ''}`}*/}
                    {/*    onClick={handleStartScan}*/}
                    {/*    disabled={scanStatus.isScanning}*/}
                    {/*>*/}
                    {/*    {scanStatus.isScanning ? 'Scanning...' : 'Start Scan'}*/}
                    {/*</Button>*/}
                </div>
            </div>
        </Container>
    );
}