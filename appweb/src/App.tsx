import React, {useState} from 'react';
import './App.css';
import {Button, Col, Container, Form, Table, Toast} from "react-bootstrap";
import {BASE_URL} from "./config";
import axios, {AxiosError} from "axios";
import {SendSmsForm, SmsDto} from "./Model";


function App() {
  const [validated, setValidated] = useState<boolean>(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null)
  const [phoneError, setPhoneError] = useState<boolean>(false)
  const [input, setInput] = useState<SendSmsForm>(new SendSmsForm())
  const [response, setResponse] = useState<SmsDto>()

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
    axios.post<SmsDto>(`${BASE_URL}/sms`, formData)
      .then(res => {
        console.log(res)
        setResponse(res.data)
      })
      .catch((err: AxiosError) => {
        let msg = err.toString();
        if (err.response) {
          if (err.response.status === 401)
            msg = 'Incorrect username or password!'
          else
            msg = err.response.statusText
        }
        setToastMsg(msg)
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
              <Form.Control type="text" name="username" onChange={handleInputChange} required/>
              <Form.Control.Feedback type="invalid">Username is required</Form.Control.Feedback>
            </Form.Group>

            <Form.Group as={Col} controlId="formPassword">
              <Form.Label>Password</Form.Label>
              <Form.Control type="password" name="password" onChange={handleInputChange} required/>
              <Form.Control.Feedback type="invalid">Password is required</Form.Control.Feedback>
            </Form.Group>
          </Form.Row>

          <Form.Group controlId="formNumbers">
            <Form.Label>Phone number list</Form.Label>
            <Form.Control type="tel" name="phones" onChange={handleInputChange} required isInvalid={phoneError}/>
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

        <div id="alert-toast">
          <Toast className="text-light bg-danger"
                 onClose={() => setToastMsg(null)} show={!!toastMsg} delay={3000} autohide>
            <Toast.Body>{toastMsg}</Toast.Body>
          </Toast>
        </div>

        {response !== undefined ?
          <Table id="result-table" bordered>
            <thead>
            <tr>
              <th>#</th>
              <th>Phone Number</th>
              <th>Status</th>
            </tr>
            </thead>
            <tbody>
            {
              response.results.map((result, idx) => (
                <tr key={idx}>
                  <td>{idx}</td>
                  <td>{result.phone}</td>
                  <td>{result.status}</td>
                </tr>
              ))
            }
            </tbody>
          </Table> : null}
      </Container>
    </div>
  );
}

export default App;
