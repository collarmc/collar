package team.catgirl.collar.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharSink;
import io.mikael.urlbuilder.UrlBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.ErrorDataEncoderException;
import team.catgirl.collar.utils.Utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP request
 */
public final class Request {

    /**
     * Method of the request
     */
    public final HttpMethod method;

    /**
     * URI of the request
     */
    public final URI uri;

    /**
     * Content to post as JSON body
     */
    public final Object content;

    /**
     * Request headers
     */
    public final Map<String, String> headers;

    /**
     * Content to post as form body
     */
    public final Map<String, String> form;

    private Request(HttpMethod method, URI uri, Object content, Map<String, String> headers, Map<String, String> form) {
        this.method = method;
        this.uri = uri;
        this.content = content;
        this.headers = headers;
        this.form = form;
    }

    /**
     * Test if request will be secure (e.g. Https)
     * @return secure
     */
    public boolean isSecure() {
        return "https".equalsIgnoreCase(uri.getScheme());
    }

    HttpRequest create() {
        if (method == null) {
            throw new IllegalStateException("method not set");
        }
        ByteBuf byteBuf;
        try {
            byteBuf = this.content == null ? Unpooled.EMPTY_BUFFER : Unpooled.copiedBuffer(Utils.jsonMapper().writeValueAsBytes(content));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (content != null) {
            headers.put("Content-Type", "application/json");
        }
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri.toString(), byteBuf);
        headers.forEach((name, value) -> request.headers().add(name, value));
        if (form != null) {
            request.headers().add("Content-Type","application/x-www-form-urlencoded");
            HttpPostRequestEncoder encoder;
            try {
                encoder = new HttpPostRequestEncoder(request, true);
            } catch (ErrorDataEncoderException e) {
                throw new IllegalStateException(e);
            }
            form.forEach((name, value) -> {
                try {
                    encoder.addBodyAttribute(name, value);
                } catch (ErrorDataEncoderException e) {
                    throw new IllegalArgumentException(e);
                }
            });
            try {
                return encoder.finalizeRequest();
            } catch (ErrorDataEncoderException e) {
                throw new IllegalStateException(e);
            }
        }
        return request;
    }

    /**
     * Create a new request builder from a url
     * @param builder url
     * @return builder
     */
    public static Builder url(UrlBuilder builder) {
        return new Builder(builder.toUri());
    }

    /**
     * Create a new request from a url
     * @param url url
     * @return builder
     */
    public static Builder url(URL url) {
        try {
            return new Builder(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create a new request builder from a url
     * @param url of request
     * @return builder
     */
    public static Builder url(String url) {
        return url(UrlBuilder.fromString(url));
    }

    public static class Builder {
        private final Map<String, String> headers = new HashMap<>();
        private final URI uri;

        private Builder(URI uri) {
            this.uri = uri;
        }

        /**
         * Specify a request header
         * @param name of header
         * @param value of header
         * @return builder
         */
        public Builder addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /**
         * Authenticate using basic auth
         * @param username to auth with
         * @param password to auth with
         * @return builder
         */
        public Builder basicAuth(String username, String password) {
            headers.put("Authorization", "Basic " + BaseEncoding.base64().encode((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
            return this;
        }

        /**
         * Create GET request
         * @return request
         */
        public Request get() {
            return new Request(HttpMethod.GET, uri, null, headers, null);
        }

        /**
         * Create a form POST request
         * @param form data
         * @return request
         */
        public Request postForm(Map<String, String> form) {
            return new Request(HttpMethod.POST, uri, null, headers, form);
        }

        /**
         * Create a JSON POST request
         * @param content to map to json
         * @return request
         */
        public Request postJson(Object content) {
            headers.put("Content-Type", "application/json");
            return new Request(HttpMethod.POST, uri, content, headers, null);
        }

        /**
         * Create a WebSocket request
         * @return request
         */
        public Request ws() {
            return new Request(null, uri, null, headers, null);
        }
    }
}
