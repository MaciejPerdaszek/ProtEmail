import "../stylings/Scanlog.css";
import {Container, FormGroup, Label, Table} from "reactstrap";
import {ScanLogService} from "../api/ScanLogService.js";
import {useEffect, useState} from "react";
import {MailboxService} from "../api/MailboxService.js";
import {toast} from "react-toastify";

export function ScanLog({user}) {
    const [logs, setLogs] = useState([]);
    const [mailboxes, setMailboxes] = useState([]);
    const [selectedMailbox, setSelectedMailbox] = useState('all');

    const fetchLogs = async (mailboxId = null) => {
        try {
            const data = await ScanLogService.fetchScanLogs(mailboxId);
            console.log(data);
            setLogs(data);
            return data;
        } catch (error) {
            console.error('Error fetching scan logs:', error);
            toast.error('Failed to fetch scan logs');
            return [];
        }
    };

    const fetchMailboxes = async () => {
        try {
            const data = await MailboxService.fetchMailboxes(user.sub);
            console.log(data);
            setMailboxes(data);
            return data;
        } catch (error) {
            console.error('Error fetching mailboxes:', error);
            toast.error('Failed to fetch mailboxes');
            return [];
        }
    };

    useEffect(() => {
        fetchMailboxes();
        fetchLogs();
    }, []);

    const handleMailboxChange = (e) => {
        const value = e.target.value;
        setSelectedMailbox(value);
        if (value === 'all') {
            fetchLogs();
        } else {
            console.log('Selected mailbox:', value);
            fetchLogs(value);
        }
    };

    return (
        <Container className="scanlog-container">
            <Container className="scanlog-content">
                <Container className="scanlog-header">
                    <h1 className="scanlog-title">Scan Logs & Reports</h1>
                    <p className="scanlog-subtitle">View all scanning activities and results</p>
                </Container>

                <Container className="table-card">
                    <Container className="card-header card-header-flex">
                        <h2 className="card-title">Scanning History</h2>
                        <FormGroup className="form-group">
                            <Label className="form-label">Choose mailbox scan logs</Label>
                            <select
                                className="form-input"
                                value={selectedMailbox}
                                onChange={handleMailboxChange}
                            >
                                <option value="all">All Mailboxes</option>
                                {mailboxes.map((mailbox) => (
                                    <option key={mailbox.id} value={mailbox.id}>
                                        {mailbox.email}
                                    </option>
                                ))}
                            </select>
                        </FormGroup>
                    </Container>

                    <Container className="table-container">
                        <Table className="scanlog-table">
                            <thead>
                            <tr>
                                <th>ID</th>
                                <th>Sender</th>
                                <th>Subject</th>
                                <th>Scan Date</th>
                                <th>Scan Status</th>
                                <th>Comment</th>
                            </tr>
                            </thead>
                            <tbody>
                            {logs.map((log) => (
                                <tr key={log.id}>
                                    <td>{log.id}</td>
                                    <td>{log.sender}</td>
                                    <td>{log.subject}</td>
                                    <td>{log.scanDate}</td>
                                    <td>
                                        <span className={`status-badge status-${log.scanStatus.toLowerCase()}`}>
                                            {log.scanStatus}
                                        </span>
                                    </td>
                                    <td>{log.comment}</td>
                                </tr>
                            ))}
                            </tbody>
                        </Table>
                    </Container>
                </Container>
            </Container>
        </Container>
    );
}