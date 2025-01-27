import {useEffect, useState} from 'react';
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { Container, Button } from 'reactstrap';
import { Mail, Clock, AlertTriangle, Undo } from 'lucide-react';
import { useScanningStore } from '../store/scannigStore.js';
import '../stylings/Mailbox.css';

export function Mailbox({ user }) {
    const { email } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const [isButtonDisabled, setIsButtonDisabled] = useState(false);
    const { mailbox = {} } = location.state || {};

    const {scannedMailboxes, initializeWebSocket, disconnectMailbox, synchronizeState} = useScanningStore();

    useEffect(() => {
        synchronizeState(user);
    }, []);

    const mailboxKey = `${email}_${user.sub}`;
    const currentMailboxState = scannedMailboxes[mailboxKey] || {
        isScanning: false,
        lastScan: null,
        emailsScanned: 0,
        threatsFound: 0
    };

    const handleStartScan = async () => {
        if (isButtonDisabled) return;

        setIsButtonDisabled(true);
        try {
            const mailboxConfig = {
                protocol: "imap",
                host: mailbox.type,
                port: "993",
                username: email,
                userId: user.sub,
            };

            await initializeWebSocket(email, mailboxConfig);
        } catch (error) {
            console.error('Error during connection setup:', error);
            disconnectMailbox(email, user.sub);
        }

        setTimeout(() => {
            setIsButtonDisabled(false);
        }, 5000);
    };

    const handleStopScan = () => {
        if (isButtonDisabled) return;

        setIsButtonDisabled(true);
        disconnectMailbox(email, user.sub);

        setTimeout(() => {
            setIsButtonDisabled(false);
        }, 5000);
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
                            <Mail className="status-icon"/>
                            <Container className="status-info">
                                <span className="status-label">Emails Scanned:</span>
                                <span>{currentMailboxState.emailsScanned}</span>
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
                            disabled={isButtonDisabled}
                        >
                            {isButtonDisabled ? 'Please wait...' : 'Stop Scan'}
                        </Button>
                    ) : (
                        <Button
                            className="scan-button"
                            onClick={handleStartScan}
                            disabled={isButtonDisabled}
                        >
                            {isButtonDisabled ? 'Please wait...' : 'Start Scan'}
                        </Button>
                    )}
                </Container>
            </Container>
        </Container>
    );
}