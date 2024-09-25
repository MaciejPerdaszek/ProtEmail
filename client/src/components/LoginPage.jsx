import React, { useEffect, useState } from 'react';
import { Button, Container } from 'reactstrap';
import { useCookies } from 'react-cookie';

export function LoginPage()  {

    const [authenticated, setAuthenticated] = useState(false);
    const [user, setUser] = useState(undefined);
    const [cookies] = useCookies(['XSRF-TOKEN']);

    useEffect(() => {
        fetch('/api/user', { credentials: 'include' })
            .then(response => response.text())
            .then(body => {
                if (body === '') {
                    setAuthenticated(false);
                } else {
                    setUser(JSON.parse(body));
                    setAuthenticated(true);
                }
            });
    }, [setAuthenticated, setUser])

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

    const message = user ?
        <h2>Welcome, {user.name}!</h2> :
        <p>Please log in to manage account</p>;


    const button = authenticated ?
        <div>
            <Button color="link" onClick={logout}>Logout</Button>
        </div> :
        <Button color="primary" onClick={login}>Login</Button>;


    return (
        <div>
            <Container fluid>
                {message}
                {button}
            </Container>
        </div>
    );
}
