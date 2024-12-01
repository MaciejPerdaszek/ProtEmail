import React, {useState} from 'react';
import '../stylings/Settings.css';
import {Form, FormGroup, Label, Input, Button, Container} from 'reactstrap';
import {Mail, Globe, Shield, MessageSquareWarning} from 'lucide-react';

export function SettingsCom() {
    const [settings, setSettings] = useState({
        emailsToScan: 100,
        language: 'en',
        notificationsEmail: true,
        notificationsApp: true,
        autoScan: true
    });

    const handleInputChange = (e) => {
        const {name, value, type, checked} = e.target;
        setSettings(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        console.log('Settings updated:', settings);
    };

    return (
        <Container className="dashboard-container">
            <Container className="dashboard-content">
                <h1 className="dashboard-title">Settings</h1>
                <p className="dashboard-subtitle">Customize your application preferences</p>

                <Container className="dashboard-split form-bottom">
                    <Container className="dashboard-card">
                        <h2 className="card-title mailbox-detail-title">
                            <Mail className="mailbox-icon"/>
                            Email Scanning Settings
                        </h2>
                        <Container className="form-container">
                            <Form onSubmit={handleSubmit}>
                                <FormGroup className="form-group">
                                    <Label className="form-label">
                                        Number of emails to scan
                                    </Label>
                                    <Input
                                        type="number"
                                        name="emailsToScan"
                                        className="form-input"
                                        value={settings.emailsToScan}
                                        onChange={handleInputChange}
                                        min="1"
                                        max="1000"
                                    />
                                </FormGroup>
                                <FormGroup className="form-group">
                                    <Input
                                        type="checkbox"
                                        name="notificationsEmail"
                                        className="form-checkbox"
                                        checked={settings.notificationsEmail}
                                        onChange={handleInputChange}
                                    />
                                    <Label className="mailbox-detail-title">
                                        <Shield className="mailbox-icon"/>
                                        Auto-scan new emails
                                    </Label>
                                </FormGroup>
                                <Button type="submit" className="form-button">
                                    Save Scan Settings
                                </Button>
                            </Form>
                        </Container>
                    </Container>

                    <Container className="dashboard-card">
                        <h2 className="card-title mailbox-detail-title">
                            <Globe className="mailbox-icon"/>
                            Application Settings
                        </h2>
                        <Container className="form-container">
                            <Form onSubmit={handleSubmit}>
                                <FormGroup className="form-group">
                                    <Label className="form-label">
                                        Language
                                    </Label>
                                    <select
                                        name="language"
                                        className="form-input"
                                        value={settings.language}
                                        onChange={handleInputChange}
                                    >
                                        <option value="en">English</option>
                                        <option value="pl">Polski</option>
                                    </select>
                                </FormGroup>
                                <FormGroup className="form-group">
                                    <Input
                                        type="checkbox"
                                        name="notificationsApp"
                                        className="form-checkbox"
                                        checked={settings.notificationsApp}
                                        onChange={handleInputChange}
                                    />
                                    <Label className="mailbox-detail-title">
                                        <MessageSquareWarning className="mailbox-icon"/>
                                        Enable Notifications
                                    </Label>
                                </FormGroup>
                                <Button type="submit" className="form-button">
                                    Save App Settings
                                </Button>
                            </Form>
                        </Container>
                    </Container>
                </Container>
            </Container>
        </Container>
    );
}