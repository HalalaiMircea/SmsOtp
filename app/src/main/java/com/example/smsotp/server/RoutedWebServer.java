package com.example.smsotp.server;

/*
 * #%L
 * NanoHttpd-Samples
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smsotp.server.handlers.ApiHandler;
import com.example.smsotp.server.handlers.DefaultHandler;
import com.example.smsotp.server.handlers.IndexHandler;
import com.example.smsotp.server.handlers.RestHandler;
import com.example.smsotp.sql.AppDatabase;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * Modified from RouterNanoHttpd artifact for SmsOTP project
 *
 * @author vnnv
 * @author ritchieGitHub
 * @author MirceaHalalai
 */
public class RoutedWebServer extends NanoHTTPD {
    private static final String TAG = "SMSOTP_NanoHTTPD";
    public static AppDatabase database;

    static {
        mimeTypes().put("json", "application/json");
    }

    private final Context context;
    private final IRoutePrioritizer routePrioritizer;
    private UriResource error404Url;

    public RoutedWebServer(Context context, int port) {
        super(port);
        RoutedWebServer.database = AppDatabase.getInstance(context);
        this.context = context;
        this.routePrioritizer = new RoutePrioritizer(NotImplementedHandler.class);

        addMappings();
    }

    public static String normalizeUri(String value) {
        if (value == null) return null;

        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;

    }

    /**
     * Routes of the server.
     */
    public void addMappings() {
        setNotFoundHandler(DefaultHandler.class, context);
        addRoute("/", IndexHandler.class, context);
        addRoute("/api/sms", ApiHandler.class, context);
    }

    public void addRoute(String url, Class<?> handler, Object... initParameter) {
        routePrioritizer.addRoute(url, 100, handler, initParameter);
    }

    public <T extends UriResponder> void setNotImplementedHandler(Class<T> handler) {
        routePrioritizer.setNotImplemented(handler);
    }

    public <T extends UriResponder> void setNotFoundHandler(Class<T> handler, Object... initParameter) {
        error404Url = new UriResource(null, 100, handler, initParameter);
    }

    public void removeRoute(String url) {
        routePrioritizer.removeRoute(url);
    }

    /**
     * Search in the mappings if the given url matches some of the rules. If
     * there are more than one marches returns the rule with less parameters
     * e.g. mapping 1 = /user/:id mapping 2 = /user/help . If the incoming uri
     * is www.example.com/user/help - mapping 2 is returned. If the incoming
     * uri is www.example.com/user/3232 - mapping 1 is returned
     */
    @Override
    public Response serve(IHTTPSession session) {
        // Try to find match
        String work = normalizeUri(session.getUri());
        Map<String, String> params = null;
        UriResource uriResource = error404Url;
        for (UriResource u : routePrioritizer.getPrioritizedRoutes()) {
            params = u.match(work);
            if (params != null) {
                uriResource = u;
                break;
            }
        }
        return uriResource.process(params, session);
    }

    public interface IRoutePrioritizer {

        void addRoute(String url, int priority, Class<?> handler, Object... initParameter);

        void removeRoute(String url);

        Collection<UriResource> getPrioritizedRoutes();

        void setNotImplemented(Class<?> notImplemented);
    }

    public abstract static class UriResponder {
        protected UriResource uriResource;
        protected Map<String, String> pathParams;
        protected IHTTPSession session;

        public UriResponder(UriResource uriResource, Map<String, String> pathParams, IHTTPSession session) {
            this.uriResource = uriResource;
            this.pathParams = pathParams;
            this.session = session;
        }

        public abstract Response doGet();

        public abstract Response doPut();

        public abstract Response doPost();

        public abstract Response doDelete();

        public abstract Response doOther(Method method);
    }

    public static class NotImplementedHandler extends UriResponder {

        public NotImplementedHandler(UriResource uriResource, Map<String, String> pathParams,
                                     IHTTPSession session) {
            super(uriResource, pathParams, session);
        }

        @Override
        public Response doGet() {
            return getResponse();
        }

        @Override
        public Response doPut() {
            return getResponse();
        }

        @Override
        public Response doPost() {
            return getResponse();
        }

        @Override
        public Response doDelete() {
            return getResponse();
        }

        @Override
        public Response doOther(Method method) {
            return getResponse();
        }

        private Response getResponse() {
            return newFixedLengthResponse("<html><body><h2>The uri is mapped in the router," +
                    " but no handler is specified.<br> Status: Not implemented!</h3></body></html>");
        }
    }

    @SuppressWarnings("RegExpRedundantEscape")
    public static class UriResource implements Comparable<UriResource> {

        private static final Pattern PARAM_PATTERN = Pattern.compile("(?<=(^|/)):[a-zA-Z0-9_-]+(?=(/|$))");

        private static final String PARAM_MATCHER = "([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=\\s]+)";

        private static final Map<String, String> EMPTY = Collections.unmodifiableMap(new HashMap<>());

        private final String uri;

        private final Pattern uriPattern;
        @NonNull private final Class<?> handler;
        private final Object[] initParameter;
        private final List<String> uriParams = new ArrayList<>();
        private int priority;

        public UriResource(String uri, int priority, @NonNull Class<?> handler, Object... initParameter) {
            this(uri, handler, initParameter);
            this.priority = priority + uriParams.size() * 1000;
        }

        public UriResource(String uri, @NonNull Class<?> handler, Object... initParameter) {
            this.handler = Objects.requireNonNull(handler);
            this.initParameter = initParameter;
            if (uri != null) {
                this.uri = normalizeUri(uri);
                parse();
                this.uriPattern = createUriPattern();
            } else {
                this.uriPattern = null;
                this.uri = null;
            }
        }

        private void parse() {
        }

        private Pattern createUriPattern() {
            String patternUri = uri;
            Matcher matcher = PARAM_PATTERN.matcher(patternUri);
            int start = 0;
            while (matcher.find(start)) {
                uriParams.add(patternUri.substring(matcher.start() + 1, matcher.end()));
                patternUri = patternUri.substring(0, matcher.start()) + PARAM_MATCHER +
                        patternUri.substring(matcher.end());
                start = matcher.start() + PARAM_MATCHER.length();
                matcher = PARAM_PATTERN.matcher(patternUri);
            }
            return Pattern.compile(patternUri);
        }

        public Response process(Map<String, String> urlParams, IHTTPSession session) {
            String error;
            try {
                if (UriResponder.class.isAssignableFrom(handler)) {
                    UriResponder responder = (UriResponder) handler.getConstructor(
                            UriResource.class, Map.class, IHTTPSession.class)
                            .newInstance(this, urlParams, session);

                    // Check if it's a RestHandler and call error checkup method
                    if (RestHandler.class.isAssignableFrom(handler)) {
                        final Response response = ((RestHandler) responder).checkErrors();
                        if (response != null) return response;
                    }
                    switch (session.getMethod()) {
                        case GET:
                            return responder.doGet();
                        case POST:
                            return responder.doPost();
                        case PUT:
                            return responder.doPut();
                        case DELETE:
                            return responder.doDelete();
                        default:
                            return responder.doOther(session.getMethod());
                    }
                } else {
                    Object object = handler.newInstance();
                    return newFixedLengthResponse(Status.OK, "text/plain",
                            "Return: " + handler.getCanonicalName() + ".toString() -> " + object
                    );
                }
            } catch (Exception e) {
                error = "Error: " + e.getClass().getName() + " : " + e.getMessage();
                Log.e(TAG, error, e);
            }
            return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", error);
        }

        @NonNull
        @Override
        public String toString() {
            return "UrlResource{uri='" + (uri == null ? "/" : uri) +
                    "', urlParts=" + uriParams + '}';
        }

        public String getUri() {
            return uri;
        }


        public <T> T initParameter(Class<T> paramClazz) {
            return initParameter(0, paramClazz);
        }

        public <T> T initParameter(int parameterIndex, Class<T> paramClazz) {
            if (initParameter.length > parameterIndex) {
                return paramClazz.cast(initParameter[parameterIndex]);
            }
            Log.e(TAG, "init parameter index not available " + parameterIndex);
            return null;
        }

        public Map<String, String> match(String url) {
            Matcher matcher = uriPattern.matcher(url);
            if (matcher.matches()) {
                if (uriParams.size() > 0) {
                    Map<String, String> result = new HashMap<>();
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        result.put(uriParams.get(i - 1), matcher.group(i));
                    }
                    return result;
                } else {
                    return EMPTY;
                }
            }
            return null;
        }

        @Override
        public int compareTo(UriResource that) {
            if (that == null) {
                return 1;
            } else return Integer.compare(this.priority, that.priority);
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

    }

    public static class RoutePrioritizer implements IRoutePrioritizer {
        protected final Collection<UriResource> mappings = new PriorityQueue<>();
        protected @NonNull Class<?> notImplemented;

        public RoutePrioritizer(@NonNull Class<?> notImplemented) {
            this.notImplemented = Objects.requireNonNull(notImplemented);
        }

        @Override
        public void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
            if (url != null) {
                if (handler != null) {
                    mappings.add(new UriResource(url, priority + mappings.size(), handler, initParameter));
                } else {
                    mappings.add(new UriResource(url, priority + mappings.size(), notImplemented));
                }
            }
        }

        public void removeRoute(String url) {
            String uriToDelete = normalizeUri(url);
            Iterator<UriResource> iterator = mappings.iterator();
            while (iterator.hasNext()) {
                UriResource uriResource = iterator.next();
                if (uriToDelete.equals(uriResource.getUri())) {
                    iterator.remove();
                    break;
                }
            }
        }

        @Override
        public Collection<UriResource> getPrioritizedRoutes() {
            return Collections.unmodifiableCollection(mappings);
        }

        @Override
        public void setNotImplemented(@NonNull Class<?> handler) {
            notImplemented = handler;
        }
    }
}

