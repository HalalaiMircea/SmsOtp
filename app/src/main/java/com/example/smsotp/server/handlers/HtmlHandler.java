package com.example.smsotp.server.handlers;

import com.example.smsotp.server.RoutedWebServer;
import com.example.smsotp.server.RoutedWebServer.UriResource;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Base handler for simple html endpoints, like index or error
 */
public abstract class HtmlHandler extends RoutedWebServer.UriResponder {


    public HtmlHandler(UriResource uriResource, Map<String, String> pathParams, IHTTPSession session) {
        super(uriResource, pathParams, session);
    }

    public abstract Response doGet();

    @Override
    public Response doPut() {
        return doGet();
    }

    @Override
    public Response doPost() {
        return doGet();
    }

    @Override
    public Response doDelete() {
        return doGet();
    }

    @Override
    public Response doOther(NanoHTTPD.Method method) {
        return doGet();
    }
}
