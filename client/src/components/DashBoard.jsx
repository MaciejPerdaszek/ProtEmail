import React, {useState} from "react";
import {useNavigate} from 'react-router-dom';
import "../stylings/DashBoard.css";
import {
    Form,
    FormGroup,
    Label,
    Input,
    Button, Container,
} from "reactstrap";

export function DashBoard({user}) {
    const [newEmail, setNewEmail] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [mailboxes, setMailboxes] = useState([{email: "new@wp.pl", password: "password"},
        {email: "new1@wp.pl", password: "password"}]);
    const navigate = useNavigate();

    const handleInputChange = (setter) => (e) => {
        setter(e.target.value);
    };

    const handleAddMailbox = (e) => {
        e.preventDefault();

        setMailboxes((prevMailboxes) => [
            ...prevMailboxes,
            {email: newEmail, password: newPassword},
        ]);

        setNewEmail("");
        setNewPassword("");
    };

    const handleMailboxNavigate = (mailbox) => {
        navigate(`/mailbox/${mailbox.email}`);
    };

    return (
        <Container className="dashboard-container">
            <Container className="dashboard-content">
                <Container className="dashboard-header">
                    <h1 className="dashboard-title">Streamline Your Inbox. Effortlessly.</h1>
                    <p className="dashboard-subtitle">Welcome to your mailbox dashboard</p>
                </Container>

                <Container className="dashboard-split">
                    <Container className="dashboard-card">
                        <h2 className="card-title">Add New Mailbox</h2>
                        <Container className="form-container">
                            <Form onSubmit={handleAddMailbox}>
                                <FormGroup className="form-group">
                                    <Label className="form-label">Email Address</Label>
                                    <Input
                                        type="email"
                                        value={newEmail}
                                        onChange={handleInputChange(setNewEmail)}
                                        placeholder="Enter email address"
                                        className="form-input"
                                        required
                                    />
                                </FormGroup>
                                <FormGroup className="form-group">
                                    <Label className="form-label">Password</Label>
                                    <Input
                                        type="password"
                                        value={newPassword}
                                        onChange={handleInputChange(setNewPassword)}
                                        placeholder="Enter password"
                                        className="form-input"
                                        required
                                    />
                                </FormGroup>
                                <Button type="submit" className="form-button">
                                    Add Mailbox
                                </Button>
                            </Form>
                        </Container>
                    </Container>

                    <Container className="dashboard-card">
                        <h2 className="card-title">Your Mailboxes</h2>
                        <Container className="mailbox-list-container">
                            {mailboxes.length > 0 ? (
                                <Container className="mailbox-list">
                                    {mailboxes.map((mailbox, index) => (
                                        <Container key={index} className="mailbox-item"
                                                   onClick={() => handleMailboxNavigate(mailbox)}>
                                            <span className="mailbox-email-label">Email:</span> {mailbox.email}
                                        </Container>
                                    ))}
                                </Container>
                            ) : (
                                <p className="empty-message">No mailboxes added yet.</p>
                            )}
                        </Container>
                    </Container>
                </Container>
            </Container>
        </Container>
    );
}
