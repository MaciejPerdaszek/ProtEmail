import React, {useState, useEffect, useRef, useCallback} from "react";
import {useNavigate} from 'react-router-dom';
import {Container} from "reactstrap";
import {MailboxForm} from './MailboxForm';
import {MailboxList} from './MailboxList';
import {MailboxService} from '../api/MailboxService.js';
import {toast} from 'react-toastify';
import "../stylings/DashBoard.css";

const INITIAL_FORM_STATE = {email: '', password: '', type: ''};

export function DashBoard({user}) {
    const [newMailbox, setNewMailbox] = useState(INITIAL_FORM_STATE);
    const [mailboxes, setMailboxes] = useState([]);
    const [editingMailbox, setEditingMailbox] = useState(null);
    const [showEditPopup, setShowEditPopup] = useState(false);
    const [activeDropdown, setActiveDropdown] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    const navigate = useNavigate();
    const popupRef = useRef(null);

    const fetchMailboxes = useCallback(async () => {
        setIsLoading(true);
        try {
            const data = await MailboxService.fetchMailboxes(user.sub);
            setMailboxes(data);
        } catch (error) {
            console.error('Error fetching mailboxes:', error);
        } finally {
            setIsLoading(false);
        }
    }, [user.sub]);

    useEffect(() => {
        fetchMailboxes();

        const handleClickOutside = (event) => {
            if (popupRef.current && !popupRef.current.contains(event.target)) {
                setShowEditPopup(false);
            }
            if (!event.target.closest('.mailbox-actions')) {
                setActiveDropdown(null);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [fetchMailboxes]);

    const handleInputChange = useCallback((e) => {
        const {name, value} = e.target;
        console.log(`Field ${name} changed to ${value}`); // Debug
        if (showEditPopup) {
            setEditingMailbox(prev => ({
                ...prev,
                [name]: value
            }));
        } else {
            setNewMailbox(prev => ({
                ...prev,
                [name]: value
            }));
        }
    }, [showEditPopup]);

    const handleAddMailbox = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await MailboxService.addMailbox(user.sub, newMailbox);
            await fetchMailboxes();
            setNewMailbox(INITIAL_FORM_STATE);
            toast('Mailbox added successfully!', {type: 'success'});
        } catch (error) {
            toast('Error adding mailbox. Please try again.', {type: 'error'});
        } finally {
            setIsLoading(false);
        }
    };

    const handleUpdateMailbox = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await MailboxService.updateMailbox(
                user.sub,
                editingMailbox.id,
                editingMailbox
            );
            await fetchMailboxes();
            setShowEditPopup(false);
            setEditingMailbox(null);
            toast('Mailbox updated successfully!', {type: 'success'});
        } catch (error) {
            toast('Error updating mailbox. Please try again.', {type: 'error'});
        } finally {
            setIsLoading(false);
        }
    };

    const handleDeleteMailbox = async (mailbox, e) => {
        e.stopPropagation();
        setIsLoading(true);
        try {
            await MailboxService.deleteMailbox(mailbox.id);
            await fetchMailboxes();
            setActiveDropdown(null);
            toast('Mailbox deleted successfully!', {type: 'success'});
        } catch (error) {
            toast('Error deleting mailbox. Please try again.', {type: 'error'});
        } finally {
            setIsLoading(false);
        }
    };

    const handleMailboxNavigate = useCallback((mailbox) => {
        navigate(`/mailbox/${mailbox.email}`);
    }, [navigate]);

    const toggleDropdown = useCallback((index, e) => {
        e.stopPropagation();
        setActiveDropdown(activeDropdown === index ? null : index);
    }, [activeDropdown]);

    const openEditPopup = useCallback((mailbox, e) => {
        e.stopPropagation();
        setEditingMailbox(mailbox);
        setShowEditPopup(true);
        setActiveDropdown(null);
    }, []);

    return (
        <Container className="dashboard-container">
            <Container className="dashboard-content">
                <Container className="dashboard-header">
                    <h1 className="dashboard-title">Streamline Your Inbox. Effortlessly.</h1>
                    <p className="dashboard-subtitle">Welcome to your mailbox dashboard</p>
                </Container>

                <Container className="dashboard-split">
                    <Container className="dashboard-card">
                        <h2 className="card-title">Add New Mailbox</h2>
                        <Container className="form-container">
                            <MailboxForm
                                formData={newMailbox}
                                onSubmit={handleAddMailbox}
                                onChange={handleInputChange}
                                buttonText="Add Mailbox"
                            />
                        </Container>
                    </Container>

                    <Container className="dashboard-card">
                        <h2 className="card-title">Your Mailboxes</h2>
                        <Container className="mailbox-list-container">
                            {isLoading ? (
                                <p>Loading...</p>
                            ) : mailboxes.length > 0 ? (
                                <MailboxList
                                    mailboxes={mailboxes}
                                    onEdit={openEditPopup}
                                    onDelete={handleDeleteMailbox}
                                    onNavigate={handleMailboxNavigate}
                                    activeDropdown={activeDropdown}
                                    onToggleDropdown={toggleDropdown}
                                />
                            ) : (
                                <p className="empty-message">No mailboxes added yet.</p>
                            )}
                        </Container>
                    </Container>
                </Container>
            </Container>

            {showEditPopup && (
                <Container className="popup-overlay">
                    <Container className="edit-popup" ref={popupRef}>
                        <h3 className="popup-title">Edit Mailbox</h3>
                        <MailboxForm
                            formData={editingMailbox}
                            onSubmit={handleUpdateMailbox}
                            onChange={handleInputChange}
                            buttonText="Save Changes"
                            setShowEditPopup={setShowEditPopup}
                        />
                    </Container>
                </Container>
            )}
        </Container>
    );
}