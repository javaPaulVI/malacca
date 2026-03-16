package dev.javapaul.malacca.routing;
import dev.javapaul.malacca.http.HttpMethod;

public record RouteEntry(HttpMethod httpMethod, String pathPattern, Object controller, java.lang.reflect.Method handlerMethod) {

}
