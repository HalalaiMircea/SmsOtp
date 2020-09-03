<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>SMSOTP</title>
</head>
<body>
<h1>Greetings, ${ip}. Welcome to SMSOTP web interface!</h1>
<form action="/api/sms" method="post">
    <label for="uname-field">Username</label><br>
    <input id="uname-field" name="username" type="text"><br>
    <label for="pass-field">Password</label><br>
    <input id="pass-field" name="password" type="password"><br>
    <label for="message-field">Message</label><br>
    <input id="message-field" name="message" type="text"><br>
    <label for="phones-field">Phone Number</label><br>
    <input id="phones-field" name="phones" type="number"><br>
    <button type="submit">Send Command</button>
</form>
</body>
</html>