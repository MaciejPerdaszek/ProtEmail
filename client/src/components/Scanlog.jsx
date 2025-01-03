import React, { useState, useEffect } from 'react';
import {Button, Container, FormGroup, Label, Table} from 'reactstrap';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { ScanLogService } from "../api/ScanLogService.js";
import { MailboxService } from "../api/MailboxService.js";
import "../stylings/Scanlog.css";

export function ScanLog({ user }) {
    const [logs, setLogs] = useState([]);
    const [mailboxes, setMailboxes] = useState([]);
    const [selectedMailbox, setSelectedMailbox] = useState('all');
    const [pageSize, setPageSize] = useState(5);
    const [pagination, setPagination] = useState({
        currentPage: 0,
        totalPages: 0,
        totalElements: 0
    });

    const fetchLogs = async (mailboxId = null, page = 0, size = pageSize) => {
        try {
            const response = await ScanLogService.fetchScanLogs(mailboxId, page, size);
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
        } catch (error) {
            console.error('Error fetching mailboxes:', error);
        }
    };

    const handleMailboxChange = (e) => {
        const value = e.target.value;
        setSelectedMailbox(value);
        setPagination(prev => ({ ...prev, currentPage: 0 }));
        if (value === 'all') {
            fetchLogs(null, 0);
        } else {
            fetchLogs(value, 0);
        }
    };

    const handlePreviousPage = () => {
        const newPage = pagination.currentPage - 1;
        if (newPage >= 0) {
            fetchLogs(selectedMailbox === 'all' ? null : selectedMailbox, newPage);
        }
    };

    const handleNextPage = () => {
        const newPage = pagination.currentPage + 1;
        if (newPage < pagination.totalPages) {
            fetchLogs(selectedMailbox === 'all' ? null : selectedMailbox, newPage);
        }
    };

    const handlePageSizeChange = (e) => {
        const newSize = parseInt(e.target.value);
        setPageSize(newSize);
        setPagination(prev => ({ ...prev, currentPage: 0 }));
        fetchLogs(selectedMailbox === 'all' ? null : selectedMailbox, 0, newSize);
    };

    useEffect(() => {
        fetchMailboxes();
        fetchLogs();
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
                                        <option value="5">5</option>
                                        <option value="10">10</option>
                                        <option value="20">20</option>
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
                                    <td>{new Date(log.scanDate).toLocaleString('en-EN', { hour: '2-digit', minute: '2-digit', year: 'numeric', month: 'long', day: 'numeric' })}</td>
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