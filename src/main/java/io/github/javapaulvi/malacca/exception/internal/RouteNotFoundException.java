package io.github.javapaulvi.malacca.exception.internal;

import io.github.javapaulvi.malacca.exception.MalaccaException;
import io.github.javapaulvi.malacca.http.HttpMethod;

// Route cannot be found for a request
public class RouteNotFoundException extends MalaccaException {
    public RouteNotFoundException(String path, HttpMethod method) {
        super("Route not found: " +method.toString()+" "+path+" 404", 404, "Not found");
    }
}
