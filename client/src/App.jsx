import {Profile} from "./components/Profile";
import {NavBar} from "./components/NavBar";
import {HomePage} from "./components/HomePage";
import {DashBoard} from "./components/DashBoard";
import {ScanLog} from "./components/Scanlog.jsx";
import {SettingsCom} from "./components/Settings.jsx";
import {Mailbox} from "./components/Mailbox.jsx";
import React, {useEffect, useState} from "react";
import {BrowserRouter as Router, Routes, Route} from "react-router-dom";
import {useCookies} from "react-cookie";

function App() {

    const [authenticated, setAuthenticated] = useState(false);
    const [user, setUser] = useState(undefined);
    const [email, setEmail] = useState(undefined);
    const [cookies] = useCookies(['XSRF-TOKEN']);

    useEffect(() => {
        fetch('/api/auth/user', {credentials: 'include'})
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
                    setEmail(data.email);
                    setAuthenticated(true);
                }
            })
    }, []);

    useEffect(() => {
        if (email !== undefined) {
            fetch('/api/users/', {
                method: 'POST',
                headers: {
                    'X-XSRF-TOKEN': cookies['XSRF-TOKEN'],
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({email}),
                credentials: 'include'
            })
                .then(response => {
                    if (response.ok) {
                        return response.json();
                    }
                })
        }
    }, [email]);

    return (
        <Router>
            <NavBar authenticated={authenticated} user={user} setAuthenticated={setAuthenticated}/>
            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/dashboard" element={<DashBoard user={user}/>}/>
                <Route path="/scanlog" element={<ScanLog/>}/>
                <Route path="/profile" element={<Profile user={user}/>}/>
                <Route path="/settings" element={<SettingsCom/>}/>
                <Route path="/mailbox/:email" element={<Mailbox/>}/>
            </Routes>
        </Router>
    );
}

export default App;
