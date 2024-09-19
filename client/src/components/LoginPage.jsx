import { useAuth0 } from "@auth0/auth0-react";

export function LoginPage() {

    const { isAuthenticated, loginWithRedirect, logout } = useAuth0();

    const LoginButton = () => {
        return <button onClick={() => loginWithRedirect()}>Log In</button>;
    };

    const LogoutButton = () => {
        return (
            <button onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>
                Log Out
            </button>
        );
    };

    return (
        <div>
            {isAuthenticated ? <LogoutButton /> : <LoginButton />}
        </div>
    );
}
