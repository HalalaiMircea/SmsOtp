<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>SMSOTP</title>
    <style type="text/css">
        body {
            font-family: Arial, serif;
        }

        h1, h2, p span {
            background-color: #525D75;
            color: white;
            font-weight: bold;
        }

        hr {
            color: #525D75;
            background-color: #525D75;
            height: 1px;
        }

        p.ex-block {
            white-space: pre-wrap;
            font-family: "Courier New", serif;
        }


    </style>
</head>
<body>
<h1>HTTP Status ${status}</h1>
<hr>
<p><span>Path</span> ${uri}</p>
<p><span>Description</span> ${description}</p>
<#if exStacktrace??>
<p><span>Exception</span></p>
<p class="ex-block">${exStacktrace}</p>
</#if>
<hr>
<h2>NanoHTTPD/2.3.1</h2>
</body>
</html>