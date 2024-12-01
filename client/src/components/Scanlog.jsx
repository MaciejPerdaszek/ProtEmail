import React from 'react';
import "../stylings/Scanlog.css";
import {Container, Table} from "reactstrap";


export function ScanLog() {

    const logs = [
        {
            id: "SCN001",
            timestamp: "2024-03-01T10:00:00",
            target: "example.com",
            type: "Full Scan",
            status: "Success",
            duration: 120,
            findings: "No vulnerabilities found"
        },
        {
            id: "SCN002",
            timestamp: "2024-03-01T11:30:00",
            target: "test.com",
            type: "Quick Scan",
            status: "Warning",
            duration: 45,
            findings: "2 medium risks detected"
        },
        {
            id: "SCN003",
            timestamp: "2024-03-01T12:15:00",
            target: "demo.net",
            type: "Custom Scan",
            status: "Error",
            duration: 60,
            findings: "Scan interrupted"
        }
    ];

    return (
        <Container className="scanlog-container">
            <Container className="scanlog-content">
                <Container className="scanlog-header">
                    <h1 className="scanlog-title">Scan Logs & Reports</h1>
                    <p className="scanlog-subtitle">View all scanning activities and results</p>
                </Container>

                <Container className="table-card">
                    <h2 className="card-title">Scanning History</h2>
                    <Container className="table-container">
                        <Table className="scanlog-table">
                            <thead>
                            <tr>
                                <th>ID</th>
                                <th>Timestamp</th>
                                <th>Target</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>Duration</th>
                                <th>Findings</th>
                            </tr>
                            </thead>
                            <tbody>
                            {logs.map((log) => (
                                <tr key={log.id}>
                                    <td>{log.id}</td>
                                    <td>{new Date(log.timestamp).toLocaleString()}</td>
                                    <td>{log.target}</td>
                                    <td>{log.type}</td>
                                    <td>
                      <span className={`status-badge status-${log.status.toLowerCase()}`}>
                        {log.status}
                      </span>
                                    </td>
                                    <td>{log.duration}s</td>
                                    <td>{log.findings}</td>
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