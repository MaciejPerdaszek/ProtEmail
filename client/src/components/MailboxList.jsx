import {Button, Container} from "reactstrap";
import {MoreVertical} from "lucide-react";
import React from "react";

export function MailboxList({mailboxes, onEdit, onDelete, onNavigate, activeDropdown, onToggleDropdown}) {

    return (
        <Container className="mailbox-list">
            {mailboxes.map((mailbox, index) => (
                <Container key={mailbox.id} className="mailbox-item">
                    <Container
                        className="mailbox-content"
                        onClick={() => onNavigate(mailbox)}
                    >
                        <span className="mailbox-email-label">Email:</span>
                        {mailbox.email}
                    </Container>
                    <Container className="mailbox-actions">
                        <Button
                            className="action-button"
                            onClick={(e) => onToggleDropdown(index, e)}
                        >
                            <MoreVertical size={20}/>
                        </Button>
                        {activeDropdown === index && (
                            <Container className="dropdown-menu">
                                <Button
                                    className="dropdown-item"
                                    onClick={(e) => onEdit(mailbox, e)}
                                >
                                    Edit
                                </Button>
                                <Button
                                    className="dropdown-item"
                                    onClick={(e) => onDelete(mailbox, e)}
                                >
                                    Delete
                                </Button>
                            </Container>
                        )}
                    </Container>
                </Container>
            ))}
        </Container>
    );

}


