import {Button, Container} from "reactstrap";
import {MoreVertical} from "lucide-react";
import { useScanningStore } from '../store/scannigStore.js';

export function MailboxList({mailboxes, onEdit, onDelete, onNavigate, activeDropdown, onToggleDropdown}) {
    const { scannedMailboxes } = useScanningStore();

    const getMailboxStatus = (mailbox) => {
        const mailboxKey = `${mailbox.email}_${mailbox.userId}`;
        return scannedMailboxes[mailboxKey]?.isScanning ? '🟢 Scanning...' : '⚪ Disconnected';
    };

    const getStatusClass = (mailbox) => {
        const mailboxKey = `${mailbox.email}_${mailbox.userId}`;
        return scannedMailboxes[mailboxKey]?.isScanning ? 'scanning' : 'disconnected';
    };

    return (
        <Container className="mailbox-list">
            {mailboxes.map((mailbox, index) => (
                <Container
                    key={mailbox.id}
                    className={`mailbox-item ${getStatusClass(mailbox)}`}
                >
                    <Container
                        className="mailbox-content"
                        onClick={() => onNavigate(mailbox)}
                    >
                        <span className="mailbox-email-label">Email:</span>
                        {mailbox.email}
                    </Container>
                    <Container className="mailbox-info">
                        <span
                            className={`connection-status ${getStatusClass(mailbox)}`}
                            title={getMailboxStatus(mailbox)}
                        >
                            {getMailboxStatus(mailbox)}
                        </span>
                    </Container>
                    <Container className="mailbox-actions">
                        <Button
                            className="action-button"
                            onClick={(e) => onToggleDropdown(index, e)}
                        >
                            <MoreVertical size={20}/>
                        </Button>
                        {activeDropdown === index && (
                            <Container className="dropdown-menu">
                                <Button
                                    className="dropdown-item"
                                    onClick={(e) => onEdit(mailbox, e)}
                                >
                                    Edit
                                </Button>
                                <Button
                                    className="dropdown-item"
                                    onClick={(e) => onDelete(mailbox, e)}
                                >
                                    Delete
                                </Button>
                            </Container>
                        )}
                    </Container>
                </Container>
            ))}
        </Container>
    );
}