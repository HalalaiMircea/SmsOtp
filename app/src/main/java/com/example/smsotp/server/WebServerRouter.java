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

import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * Modified from NanoHttpd artifact for SmsOTP project
 *
 * @author vnnv
 * @author ritchieGitHub
 * @author MirceaHalalai
 */
@SuppressWarnings("unused")
public abstract class WebServerRouter extends NanoHTTPD {

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(WebServerRouter.class.getName());
    private final UriRouter router;

    public WebServerRouter(int port) {
        super(port);
        router = new UriRouter();
    }

    public WebServerRouter(String hostname, int port) {
        super(hostname, port);
        router = new UriRouter();
    }

    public static String normalizeUri(String value) {
        if (value == null) {
            return value;
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;

    }

    /**
     * default routes, they are over writable.
     *
     * <pre>
     * router.setNotFoundHandler(GeneralHandler.class);
     * </pre>
     */

    public abstract void addMappings();

    public void addRoute(String url, Class<?> handler, Object... initParameter) {
        router.addRoute(url, 100, handler, initParameter);
    }

    public <T extends UriResponder> void setNotImplementedHandler(Class<T> handler) {
        router.setNotImplemented(handler);
    }

    public <T extends UriResponder> void setNotFoundHandler(Class<T> handler, Object... initParameter) {
        router.setNotFoundHandler(handler, initParameter);
    }

    public void removeRoute(String url) {
        router.removeRoute(url);
    }

    public void setRoutePrioritizer(IRoutePrioritizer routePrioritizer) {
        router.setRoutePrioritizer(routePrioritizer);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Try to find match
        return router.process(session);
    }

    public interface UriResponder {

        Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);

        Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);

        Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);

        Response delete(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);

        Response other(String method, UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);
    }

    public interface IRoutePrioritizer {

        void addRoute(String url, int priority, Class<?> handler, Object... initParameter);

        void removeRoute(String url);

        Collection<UriResource> getPrioritizedRoutes();

        void setNotImplemented(Class<?> notImplemented);
    }

    /**
     * General nanolet to inherit from if you provide stream data, only chucked
     * responses will be generated.
     */
    public static abstract class DefaultStreamHandler implements UriResponder {

        public abstract String getMimeType();

        public abstract IStatus getStatus();

        public abstract InputStream getData();

        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return NanoHTTPD.newChunkedResponse(getStatus(), getMimeType(), getData());
        }

        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public Response delete(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public Response other(String method, UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }
    }

    public static class NotImplementedHandler extends DefaultStreamHandler {

        public String getText() {
            return "<html><body><h2>The uri is mapped in the router, but no handler is specified. <br> Status: Not implemented!</h3></body></html>";
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public IStatus getStatus() {
            return Status.OK;
        }

        @Override
        public InputStream getData() {
            throw new IllegalStateException("this method should not be called in a text based nanolet");
        }
    }

    @SuppressWarnings("RegExpRedundantEscape")
    public static class UriResource implements Comparable<UriResource> {

        private static final Pattern PARAM_PATTERN = Pattern.compile("(?<=(^|/)):[a-zA-Z0-9_-]+(?=(/|$))");

        private static final String PARAM_MATCHER = "([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=\\s]+)";

        private static final Map<String, String> EMPTY = Collections.unmodifiableMap(new HashMap<>());

        private final String uri;

        private final Pattern uriPattern;
        private final Class<?> handler;
        private final Object[] initParameter;
        private final List<String> uriParams = new ArrayList<>();
        private int priority;

        public UriResource(String uri, int priority, Class<?> handler, Object... initParameter) {
            this(uri, handler, initParameter);
            this.priority = priority + uriParams.size() * 1000;
        }

        public UriResource(String uri, Class<?> handler, Object... initParameter) {
            this.handler = handler;
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
            String error = "General error!";
            if (handler != null) {
                try {
                    Object object = handler.newInstance();
                    if (object instanceof UriResponder) {
                        UriResponder responder = (UriResponder) object;
                        switch (session.getMethod()) {
                            case GET:
                                return responder.get(this, urlParams, session);
                            case POST:
                                return responder.post(this, urlParams, session);
                            case PUT:
                                return responder.put(this, urlParams, session);
                            case DELETE:
                                return responder.delete(this, urlParams, session);
                            default:
                                return responder.other(session.getMethod().toString(), this, urlParams, session);
                        }
                    } else {
                        return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain",
                                "Return: " + handler.getCanonicalName() + ".toString() -> " + object
                        );
                    }
                } catch (Exception e) {
                    error = "Error: " + e.getClass().getName() + " : " + e.getMessage();
                    LOG.log(Level.SEVERE, error, e);
                }
            }
            return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", error);
        }

        @SuppressWarnings("NullableProblems")
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
            LOG.severe("init parameter index not available " + parameterIndex);
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

    public static abstract class BaseRoutePrioritizer implements IRoutePrioritizer {

        protected final Collection<UriResource> mappings;
        protected Class<?> notImplemented;

        public BaseRoutePrioritizer() {
            this.mappings = newMappingCollection();
            this.notImplemented = NotImplementedHandler.class;
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
        public void setNotImplemented(Class<?> handler) {
            notImplemented = handler;
        }

        protected abstract Collection<UriResource> newMappingCollection();
    }

    public static class ProvidedPriorityRoutePrioritizer extends BaseRoutePrioritizer {

        @Override
        public void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
            if (url != null) {
                UriResource resource;
                if (handler != null) {
                    resource = new UriResource(url, handler, initParameter);
                } else {
                    resource = new UriResource(url, handler, notImplemented);
                }

                resource.setPriority(priority);
                mappings.add(resource);
            }
        }

        @Override
        protected Collection<UriResource> newMappingCollection() {
            return new PriorityQueue<>();
        }

    }

    public static class DefaultRoutePrioritizer extends BaseRoutePrioritizer {

        protected Collection<UriResource> newMappingCollection() {
            return new PriorityQueue<>();
        }
    }

    public static class InsertionOrderRoutePrioritizer extends BaseRoutePrioritizer {

        protected Collection<UriResource> newMappingCollection() {
            return new ArrayList<>();
        }
    }

    public static class UriRouter {

        private UriResource error404Url;

        private IRoutePrioritizer routePrioritizer;

        public UriRouter() {
            this.routePrioritizer = new DefaultRoutePrioritizer();
        }

        /**
         * Search in the mappings if the given url matches some of the rules. If
         * there are more than one marches returns the rule with less parameters
         * e.g. mapping 1 = /user/:id mapping 2 = /user/help . If the incoming uri
         * is www.example.com/user/help - mapping 2 is returned. If the incoming
         * uri is www.example.com/user/3232 - mapping 1 is returned
         */
        public Response process(IHTTPSession session) {
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

        @SuppressWarnings("SameParameterValue")
        private void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
            routePrioritizer.addRoute(url, priority, handler, initParameter);
        }

        private void removeRoute(String url) {
            routePrioritizer.removeRoute(url);
        }

        public void setNotFoundHandler(Class<?> handler, Object... initParameter) {
            error404Url = new UriResource(null, 100, handler, initParameter);
        }

        public void setNotImplemented(Class<?> handler) {
            routePrioritizer.setNotImplemented(handler);
        }

        public void setRoutePrioritizer(IRoutePrioritizer routePrioritizer) {
            this.routePrioritizer = routePrioritizer;
        }

    }
}

