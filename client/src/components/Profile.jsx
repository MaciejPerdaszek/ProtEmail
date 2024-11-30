import React from "react";
import '../stylings/Profile.css';
import { useState } from "react";
import {useCookies} from "react-cookie";
import { Button, Card, CardBody, CardTitle, Col, Container, Form, FormGroup, Input, Label, Row } from "reactstrap";

export function Profile ({ user }) {

    const [newEmail, setNewEmail] = useState("");
    const [cookies] = useCookies(['XSRF-TOKEN']);

    const handleEmailInputChange = (e) => {
        setNewEmail(e.target.value);
    };

    const handleEmailChange = async (e) => {
        e.preventDefault();

        try {
            const response = await fetch("/api/auth/change-email", {
                method: "POST",
                headers: {
                    'X-XSRF-TOKEN': cookies['XSRF-TOKEN'],
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ newEmail: newEmail }),
                credentials: 'include'
            });

            const data = await response.json();

            if (data.success) {
                if (data.requireRelogin) {
                    fetch('/api/auth/logout', {
                        method: 'POST', credentials: 'include',
                        headers: { 'X-XSRF-TOKEN': cookies['XSRF-TOKEN'] }
                    })
                        .then(res => res.json())
                        .then(response => {
                            window.location.href = `${response.logoutUrl}?id_token_hint=${response.idToken}`
                                + `&post_logout_redirect_uri=${window.location.origin}`;
                        });
                }
            }

            if (response.ok) {
                alert("Email changed successfully! Please check your inbox for verification email.");
                setNewEmail('');
            } else {
                alert(data.message || "Failed to change email");
            }
        } catch (error) {
            console.error("Error:", error);
            alert("Failed to change email. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Container fluid className="profile-container">
            <Row>
                <Col>
                    <Card className="profile-card">
                        <CardBody>
                            <CardTitle tag="h1" className="profile-title">
                                Profile
                            </CardTitle>
                            <p className="text-center profile-text">
                                Welcome, {user ? user.name : "Guest"}!
                            </p>
                            <p className="text-center profile-text">
                                Email: {user ? user.email : "guest@example.com"}
                            </p>

                            <Form onSubmit={handleEmailChange}>
                                <FormGroup>
                                    <Label for="new-email" className="profile-label">
                                        New Email
                                    </Label>
                                    <Input
                                        type="email"
                                        id="new-email"
                                        name="new-email"
                                        value={newEmail}
                                        onChange={handleEmailInputChange}
                                        placeholder="Enter new email"
                                        className="profile-input"
                                        required
                                    />
                                </FormGroup>
                                <Button
                                    type="submit"
                                    className="profile-button"
                                >
                                    Change Email
                                </Button>
                            </Form>
                        </CardBody>
                    </Card>
                </Col>
            </Row>
        </Container>
    );
}