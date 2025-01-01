import { useState, useEffect } from 'react';
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { Container, Button } from 'reactstrap';
import { Mail, Clock, AlertTriangle, Undo } from 'lucide-react';
import { useScanningStore } from '../store/scannigStore.js';
import '../stylings/Mailbox.css';

export function Mailbox() {
    const { email } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { mailbox = {} } = location.state || {};
    const [error, setError] = useState(null);

    const {scannedMailboxes, initializeWebSocket, disconnectMailbox, synchronizeState} = useScanningStore();

    // Synchronize state on component mount
    useEffect(() => {
        synchronizeState();
    }, []);

    const currentMailboxState = scannedMailboxes[email] || {
        isScanning: false,
        lastScan: null,
        threatsFound: 0
    };

    const handleStartScan = async () => {
        try {
            const mailboxConfig = {
                protocol: "imap",
                host: mailbox.type,
                port: "993",
                username: email,
            };

            initializeWebSocket(email, mailboxConfig);
            setError(null);
        } catch (error) {
            console.error('Error during connection setup:', error);
            setError('Failed to setup connection');
            disconnectMailbox(email);
        }
    };

    const handleStopScan = () => {
        disconnectMailbox(email);
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
                            onClick={handleStopScan}
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