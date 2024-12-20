import React from "react";
import '../stylings/Profile.css';
import {useState} from "react";
import {Button, Card, CardBody, CardTitle, Col, Container, Form, FormGroup, Input, Label, Row} from "reactstrap";
import {AuthService} from "../api/AuthService";
import {toast} from "react-toastify";

export function Profile({user}) {

    const [newEmail, setNewEmail] = useState("");

    const handleEmailInputChange = (e) => {
        setNewEmail(e.target.value);
    };

    const handleEmailChange = async (e) => {
        e.preventDefault();

        try {
            const response = await AuthService.updateEmail(newEmail);

            setNewEmail('');

            if (response.success && response.requireRelogin) {
                toast.success("Email changed successfully. Please log in again.");
                try {
                    const logoutData = await AuthService.logout();
                    window.location.href = `${logoutData.logoutUrl}?id_token_hint=${logoutData.idToken}`
                        + `&post_logout_redirect_uri=${window.location.origin}`;
                } catch (logoutError) {
                    console.error("Logout failed:", logoutError);
                }
            }
        } catch (error) {
            toast.error("Failed to change email. Please try again.");
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
                                Welcome, {user.name}!
                            </p>
                            <p className="text-center profile-text">
                                Email: {user.email}
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