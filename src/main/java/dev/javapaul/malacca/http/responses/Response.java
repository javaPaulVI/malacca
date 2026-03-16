package dev.javapaul.malacca.http.responses;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an outgoing HTTP response.
 * Wraps the response body, status code, and headers.
 * Constructed by the handler author and serialized to JSON by the dispatcher.
 *
 * <p>Example usage:</p>
 * <pre>
 * return Response.of(new UserResponse(id, "John"))
 *                .status(201)
 *                .header("X-Request-Id", "abc123");
 * </pre>
 *
 * @param <T> the type of the response body
 */
public class Response<T> {

    private int statusCode = 200;
    private final Map<String, String> headers = new HashMap<>();
    private final T body;

    /**
     * Sets the HTTP status code of the response.
     * Defaults to 200 if not called.
     *
     * @param statusCode the HTTP status code, e.g. 201, 404
     * @return this response instance for chaining
     */
    public Response<T> status(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Adds a custom header to the response.
     * The Content-Type header is set automatically by the dispatcher and does not need to be added here.
     *
     * @param key   the header name, e.g. {@code X-Request-Id}
     * @param value the header value
     * @return this response instance for chaining
     */


    public Response<T> header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * Returns the response body to be serialized to JSON.
     *
     * @return the response body
     */
    public T body() {
        return body;
    }

    /**
     * Returns the HTTP status code of the response.
     *
     * @return the status code, defaults to 200
     */
    public int statusCode() {
        return statusCode;
    }

    public boolean isJson(){
        return true;
    }

    /**
     * Returns the custom headers of the response.
     * Does not include Content-Type which is set automatically by the dispatcher.
     *
     * @return a map of custom response headers
     */
    public Map<String, String> headers() {
        return headers;
    }

    public Response(T body) {
        this.body = body;
    }

    protected Response(T body, int statusCode, Map<String, String> headers) {
        this.body = body;
        this.statusCode = statusCode;
        this.headers.putAll(headers);
    }
}