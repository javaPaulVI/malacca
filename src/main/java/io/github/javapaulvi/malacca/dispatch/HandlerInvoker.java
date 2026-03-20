package io.github.javapaulvi.malacca.dispatch;

import io.github.javapaulvi.malacca.exception.MalaccaException;
import io.github.javapaulvi.malacca.exception.internal.MalaccaInternalException;
import io.github.javapaulvi.malacca.http.Request;
import io.github.javapaulvi.malacca.http.responses.Response;
import io.github.javapaulvi.malacca.routing.RouteEntry;

import java.lang.reflect.InvocationTargetException;

class HandlerInvoker {

    public HandlerInvoker(){

    }

    public Response<?> invokeHandler(RouteEntry route, Request<?> request) {
        route.handlerMethod().setAccessible(true);
        Object result;
        try {
            result = route.handlerMethod().invoke(route.controller(), request);
        } catch (IllegalAccessException e) {
            throw new MalaccaInternalException("route.handlerMethod().setAcess() failed in HandlerInvoker class for whatever reason");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MalaccaException malaccaException){
                throw malaccaException;
            }
            throw new MalaccaInternalException("Handler threw an unexpected exception:", cause);

        }
        if (result instanceof Response<?> response) {
            return response;
        } else {
            return new Response<>(result); // wrap plain object as 200
        }
    }
}
