import React from 'react';
import {Button, Card, CardBody, Container} from 'reactstrap';
import { Mail } from 'lucide-react';
import '../stylings/VerifyEmail.css';

export function VerifyEmail() {
    return (
        <Container fluid className="verify-container">
            <Card className="verify-card">
                <CardBody>
                    <Container className="verify-content">
                        <Mail
                            className="verify-icon"
                        />
                        <h1 className="verify-title">Verify Your Email</h1>
                        <p className="verify-text">
                            We've sent a verification email to your inbox.
                            Please check your email and click the verification link to continue.
                        </p>
                        <p className="verify-subtext">
                            If you haven't received the email, please check your spam folder.
                        </p>
                    </Container>
                </CardBody>
                <Button className="btn-back" onClick={() => window.location.href = '/'}>Go Back</Button>
            </Card>
        </Container>
    );
}