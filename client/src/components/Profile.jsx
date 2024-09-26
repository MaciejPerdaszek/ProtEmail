import React from "react";
import '../stylings/Profile.css';
import main2 from '../../public/main2.jpg';

export function Profile ({ user }) {

    return (
        <div className="profile">
            <div className="background-image" style={{backgroundImage: `url(${main2})`}}>
                <div className="content">
                    <h1>Profile</h1>
                    <p className="subtitle">Welcome, {user ? user.name : "guest"}!</p>
                </div>
            </div>
        </div>
    );
}