import { Profile } from "./components/Profile";
import { NavBar } from "./components/NavBar";
import { HomePage } from "./components/HomePage";
import React, { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route} from "react-router-dom";

function App() {

    const [authenticated, setAuthenticated] = useState(false);
    const [user, setUser] = useState(undefined);

    useEffect(() => {
        fetch('/api/auth/user', { credentials: 'include' })
            .then(response => {
                if (!response.ok) {
                    setAuthenticated(false);
                    setUser(undefined);
                } else {
                    return response.json();
                }
            })
            .then(data => {
                if (data) {
                    setUser(data);
                    setAuthenticated(true);
                }
            });
    }, []);

    return (
        <Router>
            <NavBar authenticated={authenticated} user={user} setAuthenticated={setAuthenticated} />
            <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/profile" element={<Profile user={user} />} />
            </Routes>
        </Router>
    );
}

export default App;
