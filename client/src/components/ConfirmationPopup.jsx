import "../stylings/ConfirmationPopup.css";
import {Container} from "reactstrap";

export function ConfirmationPopup ({ isOpen, onClose, onConfirm, message }) {
    if (!isOpen) return null;

    return (
        <Container className="popup-overlay">
            <Container className="popup-content">
                <p>{message}</p>
                <Container className="popup-actions">
                    <button onClick={onConfirm}>Yes</button>
                    <button onClick={onClose}>No</button>
                </Container>
            </Container>
        </Container>
    );
}

