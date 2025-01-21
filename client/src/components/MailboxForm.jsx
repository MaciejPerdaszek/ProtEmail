import {Button, Form, FormGroup, Input, Label} from "reactstrap";
import {X} from "lucide-react";

export function MailboxForm({formData, onSubmit, onChange, buttonText, setShowEditPopup}) {

    return (
        <Form onSubmit={onSubmit}>
            {buttonText === 'Save Changes' && (
                <Button
                    type="button"
                    className="form-group cancel-button"
                    onClick={() => setShowEditPopup(false)}
                >
                    <X className="cancel-button"/>
                </Button>
            )}
            <FormGroup className="form-group">
                <Label className="form-label">Email Address</Label>
                <Input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={onChange}
                    placeholder="Enter email address"
                    className="form-input"
                    required
                />
            </FormGroup>
            <FormGroup className="form-group">
                <Label className="form-label">Password</Label>
                <Input
                    type="password"
                    name="password"
                    value={formData.password}
                    onChange={onChange}
                    placeholder="Enter password"
                    className="form-input"
                    required={!formData.id}
                />
            </FormGroup>
            <FormGroup className="form-group">
                <Label className="form-label">Mailbox Type</Label>
                <Input
                    type="select"
                    name="type"
                    value={formData.type}
                    className="form-input"
                    onChange={onChange}
                    required={!formData.id}
                >
                    <option value="">Select type</option>
                    <option value="wp">WP</option>
                    <option value="onet">Onet</option>
                    <option value="gmail">Gmail</option>
                </Input>
            </FormGroup>
            <Button type="submit" className="form-button">
                {buttonText}
            </Button>
        </Form>
    );
}