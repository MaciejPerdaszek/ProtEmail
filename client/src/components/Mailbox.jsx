import { useEffect } from 'react';
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
        } catch (error) {
            console.error('Error during connection setup:', error);
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

            <Container className="mailbox-detail-content">
                <Container className="mailbox-detail-card">
                    <h3 className="detail-section-title">
                        <AlertTriangle className="section-icon"/>
                        Scan Status
                    </h3>
                    <Container className="status-container">
                        <Container className="status-item">
                            <Clock className="status-icon"/>
                            <Container className="status-info">
                                <span className="status-label">Last Scan:</span>
                                <span>
                                    {currentMailboxState.lastScan
                                        ? new Date(currentMailboxState.lastScan).toLocaleString()
                                        : 'Never'}
                                </span>
                            </Container>
                        </Container>

                        <Container className="status-item">
                            <AlertTriangle className="status-icon"/>
                            <Container className="status-info">
                                <span className="status-label">Threats Found:</span>
                                <span>{currentMailboxState.threatsFound}</span>
                            </Container>
                        </Container>
                    </Container>

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
                </Container>
            </Container>
        </Container>
    );
}