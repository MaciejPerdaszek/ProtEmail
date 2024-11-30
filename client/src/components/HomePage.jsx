import React from "react";
import { useNavigate } from "react-router-dom";
import '../stylings/HomePage.css';

export function HomePage() {
    let navigate = useNavigate();

    return (
        <div className="landing-page">
            <div className="background-image" style={{ backgroundImage: `url(/main.jpg)` }}>
                <div className="content">
                    <h1>Discover</h1>
                    <p className="subtitle">Protection for your mailboxes</p>
                </div>
            </div>
        </div>
    );
}

