import React from "react";
import ReactDOM from "react-dom/client";
import { Auth0Provider } from "@auth0/auth0-react";
import "./index.css";
import App from "./App.jsx";

ReactDOM.createRoot(document.getElementById("root")).render(
    <React.StrictMode>
        <Auth0Provider
            domain="dev-i3psyoxzboeoyhet.us.auth0.com"
            clientId="XH7ZCo2FLD3sVJ0rWFe2y0pFV83C4Uqk"
            audience="https://protemail/"
            authorizationParams={{
                redirect_uri: window.location.origin
            }}
        >
            <App />
        </Auth0Provider>
    </React.StrictMode>
);
