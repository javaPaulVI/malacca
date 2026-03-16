package dev.javapaul.malacca.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;



/**
 * Represents an incoming HTTP request.
 * Wraps the raw HttpExchange and exposes a clean API for handler authors.
 * Not meant to be instantiated directly — constructed and passed by the framework.
 */
public class Request<T> {
    private final HttpMethod httpMethod;
    private final String path;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;
    private final Map<String, List<String>> headers;
    private final String rawBody;
    private final boolean hasTypedBody;
    private final T body;

    // Flat map of the body's top-level fields — used by bodyField(), bodyString() etc.
    // Populated from rawBody at construction time. Empty if the body is absent or not a JSON object.
    private final Map<String, Object> bodyFields;

    private static final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * Constructs a Request from a raw HttpExchange and pre-extracted path parameters.
     * Not meant to be called by the user — the dispatcher constructs this before invoking the handler.
     * This constructor is called when the user passes no class for body verification, then the body gets parsed as a Map.
     *
     * @param exchange   the raw HttpExchange provided by the server
     * @param pathParams path parameters extracted from the URI by the router, e.g. /users/{id} → {"id": "123"}
     * @throws JsonProcessingException if the raw body is not valid JSON
     */
    @SuppressWarnings("unchecked")
    public Request(HttpExchange exchange, Map<String, String> pathParams) throws JsonProcessingException {
        this(exchange, pathParams, (Class<T>) Map.class);
    }

    /**
     * Constructs a Request from a raw HttpExchange and pre-extracted path parameters.
     * Not meant to be called by the user — the dispatcher constructs this before invoking the handler.
     * This constructor is called when the user passes a class for body verification.
     *
     * @param exchange   the raw HttpExchange provided by the server
     * @param pathParams path parameters extracted from the URI by the router, e.g. /users/{id} → {"id": "123"}
     * @param bodyType   the class of the expected body type, e.g., UserRequest.class
     * @throws JsonProcessingException if the raw body is not valid JSON
     */
    public Request(HttpExchange exchange, Map<String, String> pathParams, Class<T> bodyType) throws JsonProcessingException {

        this.httpMethod = HttpMethod.valueOf(exchange.getRequestMethod());
        URI uri = exchange.getRequestURI();
        this.hasTypedBody = bodyType != Map.class;
        this.path = uri.getPath();
        String rawQuery = uri.getQuery();
        this.queryParams = rawQuery == null ? new HashMap<>() : parseQueryParams(rawQuery);
        this.pathParams = pathParams;
        this.headers = new HashMap<>(exchange.getRequestHeaders());
        String tempRawBody;
        try {
            tempRawBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            tempRawBody = "";
        }

        this.rawBody = tempRawBody;
        this.body = !rawBody.isEmpty() ? parseBody(bodyType) : null;

        // Parse body fields for field-level access — separate from the typed body.
        // Always produces a flat Map<String, Object> regardless of the body type T.
        // Falls back to an empty map if the body is absent or not a valid JSON object.
        this.bodyFields = !rawBody.isEmpty() ? parseBodyFields() : Collections.emptyMap();
    }

    /**
     * Parses a raw query string into a map of key-value pairs.
     * Splits on {@code &} for pairs and on the first {@code =} for key and value.
     *
     * @param rawQueries the raw query string, e.g. {@code page=2&notify=true}
     * @return a map of query parameter key-value pairs
     */
    private Map<String, String> parseQueryParams(String rawQueries) {
        Map<String, String> queryParams = new HashMap<>();
        List<String> queries = List.of(rawQueries.split("&"));
        for (String query : queries) {
            String[] keyValuePair = query.split("=", 2);
            queryParams.put(keyValuePair[0], keyValuePair.length == 2 ? keyValuePair[1] : null);
        }
        return queryParams;
    }

    /**
     * Deserializes the raw request body into an instance of the specified type.
     *
     * <p>Uses Jackson's {@link ObjectMapper} to parse the raw JSON body string into
     * the given target class. The raw body must be a valid JSON string compatible
     * with the structure of {@code type}.
     *
     * @param type the {@link Class} representing the target deserialization type;
     *             must not be {@code null}
     * @return a new instance of {@code T} populated with the deserialized body data
     * @throws JsonProcessingException if the raw body is not valid JSON, or if it
     *                                 cannot be mapped to the given type (e.g. missing
     *                                 required fields, type mismatches)
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    private T parseBody(Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(rawBody, type);
    }

    /**
     * Parses the raw body into a flat {@code Map<String, Object>} for field-level access.
     * Jackson preserves JSON types: numbers become Integer/Long/Double,
     * booleans become Boolean, strings remain String, objects become Map, arrays become List.
     * Returns an empty map if the body is not a valid JSON object.
     */
    private Map<String, Object> parseBodyFields() {
        try {
            return objectMapper.readValue(rawBody, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Returns the HTTP method of the request.
     *
     * @return the HTTP method as a {@link HttpMethod} enum value
     */
    public HttpMethod method() {
        return httpMethod;
    }

    /**
     * Returns the path of the request, excluding query string.
     *
     * @return the request path, e.g. {@code /users/123}
     */
    public String path() {
        return path;
    }

    /**
     * Returns the raw unparsed body of the request as a string.
     * Useful when the body is not JSON or requires custom parsing.
     * Returns an empty string if the request has no body.
     *
     * @return the raw request body, never null
     */
    public String rawBody() {
        return rawBody;
    }

    /**
     * Returns all values of a header by key.
     * HTTP allows the same header to appear multiple times, hence the list.
     * Returns null if the header is not present.
     * For headers that typically appear once use {@link #firstHeader(String)}.
     *
     * @param key the header name
     * @return all values for the given header key, or null if not present
     */
    public List<String> header(String key) {
        return headers.get(key);
    }

    /**
     * Returns the first value of a header by key.
     * Use this for headers that typically appear once, like Authorization.
     * For headers that can appear multiple times use {@link #header(String)}.
     *
     * <p>Example:</p>
     * <pre>
     * Accept: application/json
     * Accept: text/html
     * req.firstHeader("Accept") → "application/json"
     * </pre>
     *
     * @param key the header name, case-insensitive
     * @return the first value, or null if the header is not present
     */
    public String firstHeader(String key) {
        List<String> values = headers.get(key);
        return values != null ? values.get(0) : null;
    }

    /**
     * Returns all headers of the request as a map.
     * Each key maps to a list of values to account for duplicate header names.
     *
     * @return a map of all request headers
     */
    public Map<String, List<String>> allHeaders() {
        return headers;
    }

    /**
     * Returns the value of a path parameter by name.
     * The router extracts path parameters from the URI, e.g., /users/{id} → pathParam("id").
     * Returns null if the parameter is not present.
     *
     * @param key the path parameter name
     * @return the value of the path parameter, or null if not present
     */
    public String pathParam(String key) {
        return pathParams.get(key);
    }

    /**
     * Returns the value of a query parameter by name.
     * Returns null if the parameter is not present.
     *
     * @param key the query parameter name
     * @return the value of the query parameter, or null if not present
     */
    public String queryParam(String key) {
        return queryParams.get(key);
    }

    /**
     * Returns all path parameters as a map.
     *
     * @return a map of all path parameters
     */
    public Map<String, String> pathParams() {
        return pathParams;
    }

    /**
     * Returns all query parameters as a map.
     *
     * @return a map of all query parameters
     */
    public Map<String, String> queryParams() {
        return queryParams;
    }

    /**
     * Returns the deserialized body as an instance of the declared type T.
     * If Request<T> is Request><?> an exception is thrown.
     *
     * @return the typed body, or null if absent
     */
    public T body() {
        if (!hasTypedBody) throw new IllegalStateException(
                "body() is not available on Request<?> — use bodyMap() instead"
        );
        return body;
    }

    /**
     * Returns all top-level body fields as a map.
     * Use this when no typed body class is declared (Request<?>).
     * Jackson preserves JSON types — numbers as Integer/Long/Double,
     * booleans as Boolean, strings as String, nested objects as Map, arrays as List.
     * Returns an empty map if the request has no body.
     *
     * @return a map of all top-level body fields, never null
     */
    public Map<String, Object> bodyMap() {
        return bodyFields;
    }
}