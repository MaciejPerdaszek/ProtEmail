import {Profile} from "./components/Profile";
import {NavBar} from "./components/NavBar";
import {HomePage} from "./components/HomePage";
import {DashBoard} from "./components/DashBoard";
import {ScanLog} from "./components/Scanlog.jsx";
import {SettingsCom} from "./components/Settings.jsx";
import {Mailbox} from "./components/Mailbox.jsx";
import React, {useEffect, useState} from "react";
import {BrowserRouter as Router, Routes, Route, Navigate} from "react-router-dom";
import {ToastContainer} from "react-toastify";
import {AuthService} from "./api/AuthService";
import {UserService} from "./api/UserService.js";

function App() {

    const [authenticated, setAuthenticated] = useState(false);
    const [user, setUser] = useState(undefined);
    const [id, setId] = useState(undefined);

    useEffect(() => {
        const fetchUserData = async () => {
            try {
                const data = await AuthService.currentUser();
                if (data) {
                    console.log(data);
                    setUser(data);
                    setId(data.sub);
                    setAuthenticated(true);
                }
            } catch (error) {
                console.error('Error fetching user data:', error);
                setAuthenticated(false);
                setUser(undefined);
            }
        };

        fetchUserData();
    }, []);

    useEffect(() => {
        const fetchUser = async () => {
            if (!id || !authenticated) return;

            try {
                await UserService.addUser(id);
            } catch (error) {
                if (!error.response || error.response.data.errorCode !== 'USER_ALREADY_EXISTS') {
                    console.error('Error fetching user data:', error);
                }
            }
        };

        fetchUser();
    }, [id, authenticated]);

    return (
        <Router>
            <NavBar authenticated={authenticated} user={user} setAuthenticated={setAuthenticated} />
            <ToastContainer position="top-right" />
            <Routes>
                <Route path="/" element={<HomePage />} />
                <Route
                    path="/dashboard"
                    element={
                        authenticated ? <DashBoard user={user} /> : <Navigate to="/" replace />
                    }
                />
                <Route
                    path="/scanlog"
                    element={
                        authenticated ? <ScanLog /> : <Navigate to="/" replace />
                    }
                />
                <Route
                    path="/profile"
                    element={
                        authenticated ? <Profile user={user} /> : <Navigate to="/" replace />
                    }
                />
                <Route
                    path="/settings"
                    element={
                        authenticated ? <SettingsCom /> : <Navigate to="/" replace />
                    }
                />
                <Route
                    path="/mailbox/:email"
                    element={
                        authenticated ? <Mailbox /> : <Navigate to="/" replace />
                    }
                />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Router>
    );
}

export default App;
