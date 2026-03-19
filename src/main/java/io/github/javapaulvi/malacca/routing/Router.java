package io.github.javapaulvi.malacca.routing;

import io.github.javapaulvi.malacca.annotation.*;
import io.github.javapaulvi.malacca.annotation.*;
import io.github.javapaulvi.malacca.exception.internal.InvalidHandlerException;
import io.github.javapaulvi.malacca.exception.internal.RouteNotFoundException;
import io.github.javapaulvi.malacca.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for all routes in the application.
 * Scans registered controller instances at startup and builds a route table
 * that the Dispatcher uses to match incoming requests.
 *
 *
 */
public class Router {

    private final List<RouteEntry> routes = new ArrayList<>();
    private final Logger logger;
    private String docsUrl = null;

    public Router(String malaccaName) {
        logger = LoggerFactory.getLogger("routing.Router:"+ malaccaName);
    }



    /**
     * Scans one or more controller instances and registers their routes.
     * For each controller, reads the {@link Controller} annotation for the base path,
     * then scans all methods for HTTP method annotations ({@link GET}, {@link POST}, etc.).
     * Controllers without a {@link Controller} annotation are silently ignored.
     *
     * @param controllers one or more controller instances to register
     */
    public <T> void registerControllers(Object... controllers){
        // loop through the controllers
        for (Object controller : controllers) {

            Class<?> clazz = controller.getClass();
            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }
            Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
            String prefixController = controllerAnnotation.value().replaceAll("/+$","");

            // loop through all the methods of the controller
            for (var method : clazz.getDeclaredMethods()) {
                HttpMethod httpMethod = resolveHttpMethod(method);
                if (httpMethod == null) continue;
                validateHandlerMethod(method);
                RouteEntry routeEntry = new RouteEntry(httpMethod, prefixController + resolvePath(method), controller, method);
                if (docsUrl!=null){
                    if (routeEntry.pathPattern().equals(docsUrl) || routeEntry.pathPattern().equals("/openapi.json")){
                        throw new InvalidHandlerException("The path "+docsUrl+" and /openapi.json is reserved for the docs endpoint");
                    }
                }
                PathMatcher.validatePathPattern(routeEntry.pathPattern());
                if (routes.contains(routeEntry)) throw new InvalidHandlerException("Duplicate route: " + routeEntry);
                routes.add(routeEntry);
                logger.info("Registered route: {} {} {}.{}()", httpMethod, prefixController + resolvePath(method), controller.getClass().getSimpleName(), method.getName());

            }

        }
    }

    private void validateHandlerMethod(Method method) throws InvalidHandlerException {
        if (method.getParameterCount() != 1)
            throw new InvalidHandlerException("The Handler-Method can only have one parameter of Type Request");

        Type type = method.getGenericParameterTypes()[0];
        if (!(type instanceof ParameterizedType parameterizedType))
            throw new InvalidHandlerException("Handler parameter must be Request<?> or Request<T>");

        Type typeArg = parameterizedType.getActualTypeArguments()[0];
        if (typeArg instanceof WildcardType) {
            // Request<?> — valid, no body type
        } else if (typeArg instanceof Class<?>) {
            // Request<MyBody> — valid, body will be parsed
        } else {
            throw new InvalidHandlerException("Request type argument must be a concrete class or wildcard");
        }
    }


    private HttpMethod resolveHttpMethod(java.lang.reflect.Method method) {
        if (method.isAnnotationPresent(GET.class)) return HttpMethod.GET;
        if (method.isAnnotationPresent(POST.class)) return HttpMethod.POST;
        if (method.isAnnotationPresent(PUT.class)) return HttpMethod.PUT;
        if (method.isAnnotationPresent(DELETE.class)) return HttpMethod.DELETE;
        if (method.isAnnotationPresent(PATCH.class)) return HttpMethod.PATCH;
        return null;
    }
    private String resolvePath(java.lang.reflect.Method method) {
        if (method.isAnnotationPresent(GET.class))    return method.getAnnotation(GET.class).value().replaceAll("/+$", "");
        if (method.isAnnotationPresent(POST.class))   return method.getAnnotation(POST.class).value().replaceAll("/+$", "");
        if (method.isAnnotationPresent(PUT.class))    return method.getAnnotation(PUT.class).value().replaceAll("/+$", "");
        if (method.isAnnotationPresent(DELETE.class)) return method.getAnnotation(DELETE.class).value().replaceAll("/+$", "");
        if (method.isAnnotationPresent(PATCH.class))  return method.getAnnotation(PATCH.class).value().replaceAll("/+$", "");
        return "";
    }

    /**
     * Returns all registered routes.
     *
     * @return an unmodifiable view of all registered {@link RouteEntry} objects
     */
    public List<RouteEntry> routes() {
        return Collections.unmodifiableList(routes);
    }

    public RouteEntry getRoute(HttpMethod method, String path) {




        for (RouteEntry entry : routes()) {
            if (entry.httpMethod() == method && PathMatcher.matches(entry.pathPattern(), path))
                return entry;
        }
        throw new RouteNotFoundException(path, method);
    }

    public void enableDocs(String docsUrl) {
        this.docsUrl = docsUrl;
    }
}
