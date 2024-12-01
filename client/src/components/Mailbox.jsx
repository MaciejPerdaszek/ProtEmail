import React, { useState } from 'react';
import '../stylings/Mailbox.css';
import { Container, Form, FormGroup, Label, Input, Button } from 'reactstrap';
import { Mail, Search, Shield, Clock, AlertTriangle } from 'lucide-react';
import {useParams} from "react-router-dom";

export function Mailbox ({onClose}) {
    const { email } = useParams();
    const [scanSettings, setScanSettings] = useState({
        scanDepth: 50,
        autoScan: false,
        scanFrequency: 'daily',
        scanType: 'full'
    });

    const [scanStatus, setScanStatus] = useState({
        isScanning: false,
        lastScan: null,
        threatsFound: 0
    });

    const handleScanSettingsChange = (e) => {
        const { name, value, type, checked } = e.target;
        setScanSettings(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleStartScan = () => {
        setScanStatus(prev => ({
            ...prev,
            isScanning: true
        }));
    };

    return (
        <Container className="mailbox-detail-container">
            <Container className="mailbox-detail-header">
                <Container className="mailbox-detail-title">
                    <Mail className="mailbox-icon" />
                    <h2>{email}</h2>
                </Container>
                <Button className="close-button" onClick={onClose}>×</Button>
            </Container>

            <div className="mailbox-detail-content">
                <div className="mailbox-detail-card">
                    <h3 className="detail-section-title">
                        <Search className="section-icon" />
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
                                <Shield className="checkbox-icon" />
                                Enable Auto-Scan
                            </Label>
                        </FormGroup>
                    </Form>
                </div>

                <div className="mailbox-detail-card">
                    <h3 className="detail-section-title">
                        <AlertTriangle className="section-icon" />
                        Scan Status
                    </h3>
                    <div className="status-container">
                        <div className="status-item">
                            <Clock className="status-icon" />
                            <div className="status-info">
                                <span className="status-label">Last Scan:</span>
                                <span>{scanStatus.lastScan || 'Never'}</span>
                            </div>
                        </div>

                        <div className="status-item">
                            <AlertTriangle className="status-icon" />
                            <div className="status-info">
                                <span className="status-label">Threats Found:</span>
                                <span>{scanStatus.threatsFound}</span>
                            </div>
                        </div>
                    </div>

                    <Button
                        className={`scan-button ${scanStatus.isScanning ? 'scanning' : ''}`}
                        onClick={handleStartScan}
                        disabled={scanStatus.isScanning}
                    >
                        {scanStatus.isScanning ? 'Scanning...' : 'Start Scan'}
                    </Button>
                </div>
            </div>
        </Container>
    );
}
