package io.github.javapaulvi.malacca.routing;
import io.github.javapaulvi.malacca.http.HttpMethod;

public record RouteEntry(HttpMethod httpMethod, String pathPattern, Object controller, java.lang.reflect.Method handlerMethod) {

}
