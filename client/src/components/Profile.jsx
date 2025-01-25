import '../stylings/Profile.css';
import {useState} from "react";
import {Button, Card, CardBody, CardTitle, Col, Container, Row} from "reactstrap";
import {AuthService} from "../api/AuthService";
import {toast} from "react-toastify";
import {useScanningStore} from "../store/scannigStore.js";
import {ConfirmationPopup} from './ConfirmationPopup';

export function Profile({user}) {
    const [isPopupOpen, setIsPopupOpen] = useState(false);
    const {disconnectAllMailboxes} = useScanningStore();
    const isExternalProvider = user.sub.includes('google');

    const closePopup = () => setIsPopupOpen(false);
    const openPopup = () => setIsPopupOpen(true);

    const handleLogout = async () => {
        try {
            disconnectAllMailboxes(user.sub);
            const logoutData = await AuthService.logout();

            window.location.href = `${logoutData.logoutUrl}?id_token_hint=${logoutData.idToken}`
                + `&post_logout_redirect_uri=${window.location.origin}`;

        } catch (logoutError) {
            console.error("Logout failed:", logoutError);
        }
    };

    const handlePasswordChange = async () => {
        try {
            await AuthService.updatePassword();
            toast.success("Password change request sent. Please check your email.");
            closePopup();

            toast.info("You will be logged out in 5 seconds...");
            setTimeout(handleLogout, 5000);

        } catch (error) {
            toast.error("Failed to initiate password change. Please try again.");
        }
    };

    return (
        <Container fluid className="profile-container">
            <Row>
                <Col>
                    <Card className="profile-card">
                        <CardBody>
                            <CardTitle tag="h1" className="profile-title mb-4">
                                Profile Information
                            </CardTitle>

                            <Container className="profile-info-section">
                                <Container className="user-avatar mb-4">
                                    <img
                                        src={user.picture || '/default.png'}
                                        alt="Profile"
                                        className="rounded-circle"
                                        style={{ width: '120px', height: '120px' }}
                                        onError={(e) => e.target.src = '/default.png'}
                                    />
                                </Container>

                                <Container className="info-grid">
                                    <Container className="info-item">
                                        <Container className="info-content">
                                            <p><strong>Name:</strong> {user.name}</p>
                                            <p><strong>Nickname:</strong> {user.nickname}</p>
                                            <p><strong>Email:</strong> {user.email}</p>
                                            <p><strong>Email Verified:</strong>
                                                <span className={user.email_verified ? 'text-success' : 'text-danger'}>
                                                    {user.email_verified ? ' Yes' : ' No'}
                                                </span>
                                            </p>
                                        </Container>
                                    </Container>
                                </Container>

                                {!isExternalProvider && (
                                    <Container className="mt-4">
                                        <Button
                                            onClick={openPopup}
                                            className="profile-button"
                                        >
                                            Change Password
                                        </Button>
                                    </Container>
                                )}
                            </Container>

                            <ConfirmationPopup
                                isOpen={isPopupOpen}
                                onClose={closePopup}
                                onConfirm={handlePasswordChange}
                                message="Are you sure you want to change your password? This will require you to log out."
                            />
                        </CardBody>
                    </Card>
                </Col>
            </Row>
        </Container>
    );
}