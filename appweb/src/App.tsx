import React, {useState} from 'react';
import './App.css';
import {Button, Col, Container, Form} from "react-bootstrap";
import {BASE_URL} from "./config";
import axios from "axios";
import {SendSmsForm} from "./Model";


function App() {
    const [validated, setValidated] = useState(false);
    const [phoneError, setPhoneError] = useState(false)
    const [input, setInput] = useState(new SendSmsForm())

    const handleInputChange = (e: any) => {
        let name = e.currentTarget.name;
        let value = e.currentTarget.value;
        setInput({
            ...input,
            [name]: value
        })
        if (phoneError) setPhoneError(false)
    }

    const findFormErrors = () => {
        const regex = new RegExp("^[0-9]{10}$")
        for (let phone of input.phones.split(';')) {
            if (!regex.test(phone)) {
                // If we find error
                return true
            }
        }
        // If no error was found
        return false
    }

    function sendSmsToPhones() {
        const formData = new FormData()
        formData.append('username', input.username)
        formData.append('password', input.password)
        formData.append('message', input.message)
        input.phones.split(';').forEach(value => {
            formData.append('phones', value)
        })
        axios.post(`${BASE_URL}/sms`, formData)
            .then(res => {
                console.log(res)
                console.log(res.data)
            })
            .catch(error => {
                console.log(error)
            })
    }

    const handleSubmit = (event: any) => {
        event.preventDefault();

        const form = event.currentTarget;
        const phoneError = findFormErrors()
        if (phoneError) {
            setPhoneError(phoneError)
        } else if (form.checkValidity() === true) {
            sendSmsToPhones()
        }
        setValidated(true)
    };

    return (
        <div className="App">
            <Container>
                <Form noValidate validated={validated} onSubmit={handleSubmit}>
                    <Form.Row>
                        <Form.Group as={Col} controlId="formUsername">
                            <Form.Label>Username</Form.Label>
                            <Form.Control type="text" name="username" onChange={handleInputChange}
                                          required/>
                            <Form.Control.Feedback type="invalid">Username is required</Form.Control.Feedback>
                        </Form.Group>

                        <Form.Group as={Col} controlId="formPassword">
                            <Form.Label>Password</Form.Label>
                            <Form.Control type="password" name="password" onChange={handleInputChange}
                                          required/>
                            <Form.Control.Feedback type="invalid">Password is required</Form.Control.Feedback>
                        </Form.Group>
                    </Form.Row>

                    <Form.Group controlId="formNumbers">
                        <Form.Label>Phone number list</Form.Label>
                        <Form.Control type="tel" name="phones"
                                      onChange={handleInputChange}
                                      required isInvalid={phoneError}/>
                        <Form.Text>Must be a colon (;) separated list of 10 digit numbers</Form.Text>
                        <Form.Control.Feedback type="invalid">Phones list is invalid!</Form.Control.Feedback>
                    </Form.Group>

                    <Form.Group controlId="formMessage">
                        <Form.Label>Message</Form.Label>
                        <Form.Control as="textarea" rows={3} name="message" onChange={handleInputChange} required/>
                        <Form.Control.Feedback type="invalid">Please enter a non-blank message</Form.Control.Feedback>
                    </Form.Group>

                    <Button variant="primary" type="submit">Send SMS</Button>
                </Form>
            </Container>
        </div>
    );
}

export default App;
