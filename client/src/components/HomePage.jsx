import React from "react";
import '../stylings/HomePage.css';
import {Container} from "reactstrap";

export function HomePage() {

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

