package io.github.javapaulvi.malacca.routing;

import io.github.javapaulvi.malacca.exception.internal.InvalidHandlerException;

import java.util.*;

/**
 * Utility class for matching URL path patterns against actual request paths.
 * Supports path variables in the format {@code {variableName}}.
 * Not meant to be instantiated — all methods are static.
 */
public class PathMatcher {

    /**
     * Checks whether a path pattern matches an actual request path.
     * Segments wrapped in curly braces are treated as wildcards and match any value.
     * Literal segments must match exactly.
     *
     * @param pattern the route pattern, e.g. {@code /users/{id}}
     * @param path    the actual request path, e.g. {@code /users/123}
     * @return true if the pattern matches the path, false otherwise
     */
    public static boolean matches(String pattern, String path) {
        List<String> patternParts = Arrays.asList(pattern.split("/"));
        List<String> pathParts = Arrays.asList(path.split("/"));
        if (patternParts.size() != pathParts.size()) {
            return false;
        }
        for (int i = 0; i < patternParts.size(); i++) {
            if (patternParts.get(i).startsWith("{") && patternParts.get(i).endsWith("}")) {
                continue;
            }
            if (!patternParts.get(i).equals(pathParts.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts path parameters from a matched path.
     * For each wildcard segment in the pattern, maps the variable name to the actual value in the path.
     *
     * @param pattern the route pattern, e.g. {@code /users/{id}/orders/{orderId}}
     * @param path    the actual request path, e.g. {@code /users/123/orders/456}
     * @return a map of variable names to their values, e.g. {@code {"id": "123", "orderId": "456"}}
     */
    private static Map<String, String> extractParams(String pattern, String path) {
        Map<String, String> params = new HashMap<>();
        List<String> patternParts = Arrays.asList(pattern.split("/"));
        List<String> pathParts = Arrays.asList(path.split("/"));
        for (int i = 0; i < patternParts.size(); i++) {
            if (patternParts.get(i).startsWith("{") && patternParts.get(i).endsWith("}")) {
                params.put(patternParts.get(i).substring(1, patternParts.get(i).length() - 1), pathParts.get(i));
            }
        }
        return params;
    }

    /**
     * Attempts to match a path pattern against an actual request path.
     * Combines matching and parameter extraction into a single call.
     * Returns the extracted path parameters if the match succeeds, or empty if it does not.
     *
     * @param pattern the route pattern, e.g. {@code /users/{id}}
     * @param path    the actual request path, e.g. {@code /users/123}
     * @return an Optional containing extracted path parameters if matched, or empty if no match
     */
    public static Optional<Map<String, String>> match(String pattern, String path) {
        return matches(pattern, path) ? Optional.of(extractParams(pattern, path)) : Optional.empty();
    }

    /**
     * Validates that a path pattern is correctly formatted.
     * Checks for null, empty, missing leading slash, spaces, invalid characters and malformed path parameters.
     *
     * @param path the path pattern to validate, e.g. {@code /users/{id}}
     * @throws InvalidHandlerException if the path is invalid
     */
    public static void validatePathPattern(String path) {
        if (path == null || path.isEmpty())
            throw new InvalidHandlerException("Path must not be null or empty");
        if (!path.startsWith("/"))
            throw new InvalidHandlerException("Path must start with '/', got: " + path);
        if (path.contains(" "))
            throw new InvalidHandlerException("Path must not contain spaces, got: " + path);
        if (!path.matches("/[a-zA-Z0-9/_{}.-]*"))
            throw new InvalidHandlerException("Path contains invalid characters: " + path);
        if (path.contains("{") || path.contains("}")) {
            String[] segments = path.split("/");
            for (String segment : segments) {
                if (segment.contains("{") || segment.contains("}")) {
                    if (!segment.matches("\\{[a-zA-Z][a-zA-Z0-9]*\\}"))
                        throw new InvalidHandlerException("Malformed path parameter in: " + path + " — must be {paramName}");
                }
            }
        }
    }

    public static List<String> getParams(String pathPattern){
        validatePathPattern(pathPattern);
        String[] pathSegs = pathPattern.split("/");
        List<String> params = new ArrayList<>();
        for (String seg : pathSegs){
            if(seg.startsWith("{") && seg.endsWith("}")) params.add(seg.substring(1, seg.length()-1));
        }
        return params;
    }
}