import React from "react";
import {useNavigate} from "react-router-dom";
import '../stylings/HomePage.css';
import {Container} from "reactstrap";

export function HomePage() {
    let navigate = useNavigate();

    return (
        <Container className="landing-page">
            <Container className="background-image" style={{backgroundImage: `url(/main.jpg)`}}>
                <Container className="content">
                    <h1>Discover</h1>
                    <p className="subtitle">Protection for your mailboxes</p>
                </Container>
            </Container>
        </Container>
    );
}

