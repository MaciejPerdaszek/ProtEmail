import React, {useState, useEffect} from 'react';
import {Button, Container, FormGroup, Label, Table} from 'reactstrap';
import {ChevronLeft, ChevronRight} from 'lucide-react';
import {ScanLogService} from "../api/ScanLogService.js";
import {MailboxService} from "../api/MailboxService.js";
import {useScanningStore} from '../store/scannigStore.js';
import "../stylings/Scanlog.css";

export function ScanLog({user}) {
    const [logs, setLogs] = useState([]);
    const [mailboxes, setMailboxes] = useState([]);
    const [selectedMailbox, setSelectedMailbox] = useState('all');
    const [pageSize, setPageSize] = useState(10);
    const [pagination, setPagination] = useState({
        currentPage: 0,
        totalPages: 0,
        totalElements: 0
    });

    const stompClients = useScanningStore(state => state.stompClients);

    const fetchLogs = async (mailboxIds = null, page = 0, size = pageSize) => {
        try {
            const response = await ScanLogService.fetchScanLogs(
                mailboxIds,
                page,
                size,
                user.sub
            );
            setLogs(response.data);
            setPagination({
                currentPage: response.currentPage,
                totalPages: response.totalPages,
                totalElements: response.total
            });
        } catch (error) {
            console.error('Error fetching scan logs:', error);
        }
    };

    const fetchMailboxes = async () => {
        try {
            const data = await MailboxService.fetchMailboxes(user.sub);
            setMailboxes(data);
            const ids = data.map(mailbox => mailbox.id);
            fetchLogs(ids, 0, pageSize);
        } catch (error) {
            console.error('Error fetching mailboxes:', error);
        }
    };

    const handleMailboxChange = (e) => {
        const value = e.target.value;
        setSelectedMailbox(value);
        setPagination(prev => ({...prev, currentPage: 0}));

        if (value === 'all') {
            const allIds = mailboxes.map(mailbox => mailbox.id);
            fetchLogs(allIds, 0, pageSize);
        } else {
            fetchLogs([value], 0, pageSize);
        }
    };

    const handlePreviousPage = () => {
        const newPage = pagination.currentPage - 1;
        if (newPage >= 0) {
            const ids = selectedMailbox === 'all'
                ? mailboxes.map(mailbox => mailbox.id)
                : [selectedMailbox];
            fetchLogs(ids, newPage, pageSize);
        }
    };

    const handleNextPage = () => {
        const newPage = pagination.currentPage + 1;
        if (newPage < pagination.totalPages) {
            const ids = selectedMailbox === 'all'
                ? mailboxes.map(mailbox => mailbox.id)
                : [selectedMailbox];
            fetchLogs(ids, newPage, pageSize);
        }
    };

    const handlePageSizeChange = (e) => {
        const newSize = parseInt(e.target.value);
        setPageSize(newSize);
        setPagination(prev => ({...prev, currentPage: 0}));
        const ids = selectedMailbox === 'all'
            ? mailboxes.map(mailbox => mailbox.id)
            : [selectedMailbox];
        fetchLogs(ids, 0, newSize);
    };

    useEffect(() => {
        const subscriptions = Object.entries(stompClients).map(([mailboxKey, client]) => {
            if (client && client.connected) {
                const [email] = mailboxKey.split('_');

                return client.subscribe(`/topic/scanlog/${email}/${user.sub}`, (message) => {
                    try {
                        const newLog = JSON.parse(message.body);
                        setLogs(prevLogs => {
                            const exists = prevLogs.some(log => log.id === newLog.id);
                            if (exists) {
                                return prevLogs.map(log =>
                                    log.id === newLog.id ? newLog : log
                                );
                            } else {
                                return [newLog, ...prevLogs.slice(0, pageSize - 1)];
                            }
                        });
                    } catch (error) {
                        console.error("Error processing scan log:", error);
                    }
                });
            }
            return null;
        }).filter(Boolean);

        return () => {
            subscriptions.forEach(subscription => subscription.unsubscribe());
        };
    }, [stompClients, pageSize, user.sub]);

    useEffect(() => {
        fetchMailboxes();
    }, []);

    return (
        <Container className="scanlog-container">
            <Container className="scanlog-content">
                <Container className="scanlog-header">
                    <h1 className="scanlog-title">Scan Logs & Reports</h1>
                    <p className="scanlog-subtitle">View all scanning activities and results</p>
                </Container>

                <Container className="table-card">
                    <Container className="card-header">
                        <div className="header-content">
                            <h2 className="card-title">Scanning History</h2>
                            <div className="header-controls">
                                <FormGroup className="form-group">
                                    <Label className="form-label">Items per page</Label>
                                    <select
                                        value={pageSize}
                                        onChange={handlePageSizeChange}
                                        className="form-input"
                                    >
                                        <option value="10">10</option>
                                        <option value="20">20</option>
                                        <option value="50">50</option>
                                    </select>
                                </FormGroup>
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
                            </div>
                        </div>
                    </Container>


                    <Container className="table-container">
                        <Table className="scanlog-table">
                            <thead>
                            <tr>
                                <th style={{width: '15%'}}>Sender</th>
                                <th style={{width: '15%'}}>Subject</th>
                                <th style={{width: '20%'}}>Scan Date</th>
                                <th style={{width: '15%'}}>Threat Level</th>
                                <th style={{width: '35%'}}>Comment</th>
                            </tr>
                            </thead>
                            <tbody>
                            {logs.map((log) => (
                                <tr key={log.id}>
                                    <td className="truncate-cell">
                                        <div className="cell-content">
                                            {log.sender}
                                        </div>
                                    </td>
                                    <td className="truncate-cell">
                                        <div className="cell-content">
                                            {log.subject}
                                        </div>
                                    </td>
                                    <td className="truncate-cell">
                                        <div className="cell-content">
                                            {new Date(log.scanDate).toLocaleString('en-EN', {
                                                hour: '2-digit',
                                                minute: '2-digit',
                                                year: 'numeric',
                                                month: 'long',
                                                day: 'numeric'
                                            })}
                                        </div>
                                    </td>
                                    <td>
                                        <span className={`status-badge status-${log.threatLevel.toLowerCase().split(/\s+/)[0]}`}>
                                            {log.threatLevel}
                                        </span>
                                    </td>
                                    <td className="truncate-cell">
                                        <div className="cell-content">
                                            {log.comment}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </Table>

                        <Container className="pagination-container">
                            <Button
                                onClick={handlePreviousPage}
                                disabled={pagination.currentPage === 0}
                                className="pagination-button pagination-button-prev"
                            >
                                <ChevronLeft/>
                                Previous
                            </Button>
                            <span className="pagination-info">
                                Page {pagination.currentPage + 1} of {pagination.totalPages}
                            </span>
                            <Button
                                onClick={handleNextPage}
                                disabled={pagination.currentPage >= pagination.totalPages - 1}
                                className="pagination-button pagination-button-next"
                            >
                                Next
                                <ChevronRight/>
                            </Button>
                        </Container>
                    </Container>
                </Container>
            </Container>
        </Container>
    );
}