import React from "react";
import { Navbar } from 'reactstrap';
import '../stylings/NavBar.css';
import { Link } from "react-router-dom";
import {useCookies} from "react-cookie";

export function NavBar({ authenticated, user, setAuthenticated }) {

    const [cookies] = useCookies(['XSRF-TOKEN']);

    const login = () => {
        let port = (window.location.port ? ':' + window.location.port : '');
        if (port === ':5173') {
            port = ':8080';
        }
        // redirect to a protected URL to trigger authentication
        window.location.href = `//${window.location.hostname}${port}/api/private`;
    }

    const logout = () => {
        fetch('/api/logout', {
            method: 'POST', credentials: 'include',
            headers: { 'X-XSRF-TOKEN': cookies['XSRF-TOKEN'] }
        })
            .then(res => res.json())
            .then(response => {
                window.location.href = `${response.logoutUrl}?id_token_hint=${response.idToken}`
                    + `&post_logout_redirect_uri=${window.location.origin}`;
            });
    }

    return (
        <div>
            <Navbar color="light" light expand="md" style={{width: '100%'}}>
                <header className="navbar">
                    <div className="logo" onClick={() => {  window.location.href = '/'; }}>ProtEmail</div>
                    <nav className="nav-links">
                        <Link to="#dashboard">Dashboard</Link>
                        <Link to="#reports">Reports</Link>
                        <Link to="/profile">Profile</Link>
                        <Link to="#settings">Settings</Link>
                    </nav>
                    {authenticated ? (
                        <button className="logout-button" onClick={() => logout()}>Logout</button>
                    ) : (
                        <button className="login-button" onClick={() => login()}>Login</button>
                    )}
                </header>
            </Navbar>
        </div>
    );
}