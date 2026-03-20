package io.github.javapaulvi.malacca.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import io.github.javapaulvi.malacca.exception.internal.MalaccaInternalException;
import io.github.javapaulvi.malacca.exception.internal.RequestBodyParseException;
import io.github.javapaulvi.malacca.http.Request;
import io.github.javapaulvi.malacca.routing.PathMatcher;
import io.github.javapaulvi.malacca.routing.RouteEntry;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.lang.reflect.WildcardType;
import java.util.Map;



 class RequestBuilder {

    public RequestBuilder(){

    }

    // Public method
    @SuppressWarnings("unchecked")
    public <T> Request<T> build(HttpExchange exchange, RouteEntry route) throws RequestBodyParseException{
        Class<?> bodyType = resolveBodyType(route.handlerMethod());
        Map<String, String> pathParams = extractPathParams(route.pathPattern(), exchange.getRequestURI().getPath());
        return (Request<T>) makeRequest(exchange,pathParams,bodyType);
    }

    // Private helpers


    private Class<?> resolveBodyType(Method method) {
        ParameterizedType parameterizedType = (ParameterizedType) method.getGenericParameterTypes()[0];
        Type typeArg = parameterizedType.getActualTypeArguments()[0];
        if (typeArg instanceof WildcardType) return null; // Request<?> — use Map.class as default
        return (Class<?>) typeArg; // Request<MyBody>
    }

    private <T> Request<T> makeRequest(HttpExchange exchange, Map<String, String> pathParams, Class<T> bodyType){

        try {
            if (bodyType == null) {
                return new Request<>(exchange, pathParams);
            } else{
                return new Request<>(exchange, pathParams, bodyType);
            }
        } catch (JsonProcessingException e) {
            throw new RequestBodyParseException(e);
        }

    }

    private Map<String, String> extractPathParams(String pattern, String path){
        return PathMatcher.match(pattern, path)
                .orElseThrow(() -> new MalaccaInternalException("Internal Error, Router passed wrong record"));
    }

}