import {Navbar, Button, Container, Nav} from 'reactstrap';
import '../stylings/NavBar.css';
import {Link} from "react-router-dom";
import {AuthService} from '../api/AuthService.js';
import {useScanningStore} from "../store/scannigStore.js";

export function NavBar({authenticated, user}) {

    const {disconnectAllMailboxes} = useScanningStore();

    const login = () => {
        let port = (window.location.port ? ':' + window.location.port : '');
        if (port === ':5173') {
            port = ':8080';
        }
        // redirect to a protected URL to trigger authentication
        window.location.href = `//${window.location.hostname}${port}/api/private`;
    }

    const logout = async () => {
        try {
            disconnectAllMailboxes(user.sub);
            const data = await AuthService.logout();

            window.location.href = `${data.logoutUrl}?id_token_hint=${data.idToken}`
                + `&post_logout_redirect_uri=${window.location.origin}`;
        } catch (error) {
            console.error('Error logging out:', error);
        }
    }

    return (
        <Container>
            <Navbar className="navbar">
                <Container className="maindiv">
                    <Container className="logo" onClick={() => {
                        window.location.href = '/';
                    }}>ProtEmail</Container>
                    <Nav className="nav-links">
                        {authenticated && (
                            <>
                                <Link to="/dashboard">Dashboard</Link>
                                <Link to="/scanlog">Scan logs</Link>
                                <Link to="/profile">Profile</Link>
                            </>
                        )}
                    </Nav>
                    {authenticated ? (
                        <Button className="logout-button" onClick={() => logout()}>Logout</Button>
                    ) : (
                        <Button className="login-button" onClick={() => login()}>Login</Button>
                    )}
                </Container>
            </Navbar>
        </Container>
    );
}