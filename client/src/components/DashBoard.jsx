import {useState, useEffect, useRef, useCallback} from "react";
import {useNavigate} from 'react-router-dom';
import {Button, Container} from "reactstrap";
import {MailboxForm} from './MailboxForm';
import {MailboxList} from './MailboxList';
import {MailboxService} from '../api/MailboxService.js';
import {toast} from 'react-toastify';
import "../stylings/DashBoard.css";
import {useScanningStore} from '../store/scannigStore.js';
import {ConfirmationPopup} from './ConfirmationPopup';

const INITIAL_FORM_STATE = {email: '', password: '', type: ''};

export function DashBoard({user}) {
    const [newMailbox, setNewMailbox] = useState(INITIAL_FORM_STATE);
    const [mailboxes, setMailboxes] = useState([]);
    const [editingMailbox, setEditingMailbox] = useState(null);
    const [showEditPopup, setShowEditPopup] = useState(false);
    const [activeDropdown, setActiveDropdown] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isPopupOpen, setIsPopupOpen] = useState(false);
    const [selectedMailbox, setSelectedMailbox] = useState(null);
    const [originalEmail, setOriginalEmail] = useState(null);

    const {scannedMailboxes, initializeWebSocket, synchronizeState, disconnectMailbox, disconnectAllMailboxes} = useScanningStore();

    const navigate = useNavigate();
    const popupRef = useRef(null);

    const openPopup = (mailbox) => {
        setSelectedMailbox(mailbox);
        setIsPopupOpen(true);
    };

    const closePopup = () => {
        setIsPopupOpen(false);
        setSelectedMailbox(null);
    };

    const fetchMailboxes = useCallback(async () => {
        setIsLoading(true);
        try {
            const data = await MailboxService.fetchMailboxes(user.sub);
            setMailboxes(data);
        } catch (error) {
            console.error('Error fetching mailboxes:', error);
            toast('Error fetching mailboxes', {type: 'error'});
        } finally {
            setIsLoading(false);
        }
    }, [user.sub]);

    useEffect(() => {
        const initialize = async () => {
            await synchronizeState();

            // Przywróć połączenia WebSocket dla aktywnych skrzynek
            mailboxes.forEach(mailbox => {
                if (scannedMailboxes[mailbox.email]?.isScanning) {
                    const config = {
                        protocol: "imap",
                        host: mailbox.type,
                        port: "993",
                        username: mailbox.email,
                        userId: user.sub
                    };
                    initializeWebSocket(mailbox.email, config);
                }
            });
        };

        initialize();

        // const interval = setInterval(synchronizeState, 30000);
        // return () => clearInterval(interval);
    }, [mailboxes]);

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
            toast('Error adding mailbox: ' + error.response.data.message , {type: 'error'});
        } finally {
            setIsLoading(false);
        }
    };

    const handleUpdateMailbox = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            if (scannedMailboxes[originalEmail]?.isScanning) {
                disconnectMailbox(originalEmail);
            }

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

    const handleDeleteMailbox = async () => {
        if (!selectedMailbox) return;

        setIsLoading(true);
        try {
            if (scannedMailboxes[selectedMailbox.email]?.isScanning) {
                disconnectMailbox(selectedMailbox.email);
            }

            await MailboxService.deleteMailbox(selectedMailbox.id);
            await fetchMailboxes();
            setActiveDropdown(null);
            toast('Mailbox deleted successfully!', { type: 'success' });
        } catch (error) {
            toast('Error deleting mailbox. Please try again.', { type: 'error' });
        } finally {
            setIsLoading(false);
            closePopup();
        }
    };

    const handleMailboxNavigate = useCallback((mailbox) => {
        const scanningState = scannedMailboxes[mailbox.email] || {
            isScanning: false,
            lastScan: null,
            threatsFound: 0
        };

        navigate(`/mailbox/${mailbox.email}`, {
            state: {
                mailbox,
                meta: {
                    scanningState
                }
            }
        });
    }, [navigate, scannedMailboxes]);

    const getMailboxesWithScanningState = useCallback(() => {
        return mailboxes.map(mailbox => ({
            ...mailbox,
            scanningState: scannedMailboxes[mailbox.email] || {
                isScanning: false,
                lastScan: null,
                scannedMailboxes: 0,
                threatsFound: 0
            }
        }));
    }, [mailboxes, scannedMailboxes]);

    const toggleDropdown = useCallback((index, e) => {
        e.stopPropagation();
        setActiveDropdown(activeDropdown === index ? null : index);
    }, [activeDropdown]);

    const openEditPopup = useCallback((mailbox, e) => {
        e.stopPropagation();
        setEditingMailbox(mailbox);
        setOriginalEmail(mailbox.email);
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
                                <p className="empty-message">Loading...</p>
                            ) : mailboxes.length > 0 ? (
                                <MailboxList
                                    mailboxes={getMailboxesWithScanningState()}
                                    onEdit={openEditPopup}
                                    onDelete={openPopup}
                                    onNavigate={handleMailboxNavigate}
                                    activeDropdown={activeDropdown}
                                    onToggleDropdown={toggleDropdown}
                                />
                            ) : (
                                <p className="empty-message">No mailboxes added yet.</p>
                            )}
                        </Container>
                        <Button type="submit" className="form-button" onClick={disconnectAllMailboxes}>Stop scanning mailboxes</Button>
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
            <ConfirmationPopup
                isOpen={isPopupOpen}
                onClose={closePopup}
                onConfirm={handleDeleteMailbox}
                message={`Are you sure you want to delete mailbox ${selectedMailbox?.email}?`}
            />
        </Container>
    );
}