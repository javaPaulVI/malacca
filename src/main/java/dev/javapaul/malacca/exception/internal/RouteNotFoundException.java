package dev.javapaul.malacca.exception.internal;

import dev.javapaul.malacca.exception.MalaccaException;
import dev.javapaul.malacca.http.HttpMethod;

// Route cannot be found for a request
public class RouteNotFoundException extends MalaccaException {
    public RouteNotFoundException(String path, HttpMethod method) {
        super("Route not found: " +method.toString()+" "+path+" 404", 404, "Not found");
    }
}
