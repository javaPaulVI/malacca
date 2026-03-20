package io.github.javapaulvi.malacca.dispatch;

import com.sun.net.httpserver.HttpExchange;
import io.github.javapaulvi.malacca.exception.MalaccaException;
import io.github.javapaulvi.malacca.http.responses.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ErrorHandler {
    private final ResponseWriter responseWriter;
    private final Logger logger;


    public ErrorHandler(ResponseWriter responseWriter, String malaccaName){
        this.responseWriter = responseWriter;
        logger = LoggerFactory.getLogger("dispatch.ErrorHandler:"+ malaccaName);
    }

    public void writeError(HttpExchange exchange, MalaccaException exception){
        Response<ErrorResponse> response = exceptionToResponse(exception);
        responseWriter.writeResponse(exchange, response);
        logger.error("Internal exception message: {}",exception.internalMessage());
    }

    private Response<ErrorResponse> exceptionToResponse(MalaccaException exception){
        return new Response<>(new ErrorResponse(exception.clientMessage())).status(exception.statusCode());
    }
}
