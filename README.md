# SMSOTP

SMSOTP is an Android application used for sending SMS messages from
other devices over the HTTP protocol. The app has an embedded web server
which exposes a simple REST API for sending SMS messages. It also has a
simple SQLite database containing User and Command tables. The app also
features a UI for managing user credentials.

## Getting started

The simplest way of working on the project is to import it from Gradle.
Be sure to use Android Studio v4.0 or higher and update your SDK to API
level 30. After you setup your IDE, just select *Import from Gradle*
when first starting up Android Studio then select the `build.gradle`
file at the root directory of the project and wait for Gradle to sync
with the build script.

## Dependencies

* [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) is used as an
  embedded web server which listens to incoming requests.

* [Room ORM](https://developer.android.com/topic/libraries/architecture/room)
  is used for accessing the underlying SQLite database built into
  Android

## API description

The API is designed to be called with POST methods exclusively. This is
because each command given with a request requires authentication with a
set of username and password. It is compatible with form-data type of
request bodies, so when testing the API be sure to make a compatible
request.

I've personally tested it with [Postman](https://www.postman.com/), but
it should work with other testing software such as cURL.

### Port

The default port for the web server is 8080. It can be changed in
[WebServer class source](/app/src/main/java/com/example/smsotp/WebServer.java)

### Parameters

Your request body should have these parameters:
* username
* password
* message
* phones

For passing an array of phone numbers, repeat the phones parameter for
each number. Here's such an example in cURL:

```shell script
curl --location --request POST '192.168.34.100:8080' \
--form 'message=Hello from Postman' \
--form 'username=admin' \
--form 'password=test' \
--form 'phones=0123456789' \
--form 'phones=1234567890' \
--form 'phones=2345678901'
```

## UI

The UI is made with the modern **Single Activity** android pattern. The
activity is used only as an entry point for the UI and for each screen
or tab, **Fragments** are used. These Fragments, in conjunction with the
Android **Navigation Component**, work together to form a modern UI
structure/hierarchy.

## Android version support

The application was tested on a version as low as Lollipop 5.1. So the
required minimum SDK version is 22. Also I used Java language level 8
for lambdas. I'm disappointed the current Android SDK doesn't support
newer Java language levels, to support var for example. That's why I
might convert the project to Kotlin in the future.
