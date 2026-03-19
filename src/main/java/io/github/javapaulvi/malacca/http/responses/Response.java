package io.github.javapaulvi.malacca.http.responses;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an outgoing HTTP response.
 * Wraps the response body, status code, and headers.
 * Constructed by the handler author and serialized by the dispatcher.
 *
 * <p>Typed response — body is serialized to JSON automatically:</p>
 * <pre>
 * return Response.of(new UserResponse(id, "John"))
 *                .status(201)
 *                .header("X-Request-Id", "abc123");
 * </pre>
 *
 * <p>Unstructured response — build the JSON shape freely:</p>
 * <pre>
 * return Response.unstructured()
 *                .field("message", "created")
 *                .field("id", 123)
 *                .status(201);
 * </pre>
 *
 * @param <T> the type of the response body
 */
public class Response<T> {

    private int statusCode = 200;
    private final Map<String, String> headers = new HashMap<>();
    private final T body;

    // When true, body is ignored and bodyFields is serialized instead.
    // Set by the unstructured() factory.
    private final boolean unstructured;
    private final Map<String, Object> bodyFields;

    // -------------------------------------------------------------------------
    // Static factories
    // -------------------------------------------------------------------------

    /**
     * Creates a typed response with the given body.
     * The body is serialized to JSON automatically by the dispatcher.
     *
     * @param body the response body
     * @param <T>  the body type
     * @return a new Response wrapping the body
     */
    public static <T> Response<T> of(T body) {
        return new Response<>(body);
    }

    /**
     * Creates an unstructured response with no predefined body type.
     * Use {@link #field(String, Object)} to add fields freely.
     * Useful when the response shape is dynamic or not worth modelling as a record.
     *
     * <p>Example:</p>
     * <pre>
     * return Response.unstructured()
     *                .field("id", user.id())
     *                .field("created", true)
     *                .status(201);
     * </pre>
     *
     * @return a new unstructured Response
     */
    public static Response<Void> unstructured() {
        return new Response<>();
    }

    // -------------------------------------------------------------------------
    // Fluent API
    // -------------------------------------------------------------------------

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
     * Content-Type is set automatically by the dispatcher.
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
     * Adds a field to an unstructured response body.
     * Only available on responses created via {@link #unstructured()}.
     * Throws if called on a typed response.
     *
     * @param key   the field name
     * @param value the field value — any JSON-serializable type
     * @return this response instance for chaining
     * @throws IllegalStateException if called on a typed response
     */
    public Response<T> field(String key, Object value) {
        if (!unstructured) throw new IllegalStateException(
                "field() is only available on unstructured responses — use Response.unstructured() first"
        );
        bodyFields.put(key, value);
        return this;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the typed response body.
     * Returns null for unstructured responses — use {@link #bodyFields()} instead.
     *
     * @return the typed body, or null if unstructured
     */
    public T body() {
        return body;
    }

    /**
     * Returns the unstructured body fields map.
     * Only populated for responses created via {@link #unstructured()}.
     * Returns an empty map for typed responses.
     *
     * @return the body fields map, never null
     */
    public Map<String, Object> bodyFields() {
        return bodyFields;
    }

    /**
     * Returns true if this is an unstructured response.
     * The dispatcher uses this to decide whether to serialize {@code body} or {@code bodyFields}.
     *
     * @return true if unstructured, false if typed
     */
    public boolean isUnstructured() {
        return unstructured;
    }

    /**
     * Returns the HTTP status code of the response.
     *
     * @return the status code, defaults to 200
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns true — JSON is the default content type.
     * Subclasses like {@link PlainTextResponse} override this to return false.
     *
     * @return true
     */
    public boolean isJson() {
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

    // -------------------------------------------------------------------------
    // Constructors — package-private, use static factories
    // -------------------------------------------------------------------------

    public Response(T body) {
        this.body = body;
        this.unstructured = false;
        this.bodyFields = new HashMap<>();
    }

    private Response() {
        this.body = null;
        this.unstructured = true;
        this.bodyFields = new HashMap<>();
    }

    protected Response(T body, int statusCode, Map<String, String> headers) {
        this.body = body;
        this.statusCode = statusCode;
        this.headers.putAll(headers);
        this.unstructured = false;
        this.bodyFields = new HashMap<>();
    }
}