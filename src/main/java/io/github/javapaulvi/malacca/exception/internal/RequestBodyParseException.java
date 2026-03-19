package io.github.javapaulvi.malacca.exception.internal;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.javapaulvi.malacca.exception.MalaccaException;

// JSON body could not be parsed into Request<T>
public class RequestBodyParseException extends MalaccaException {
    public RequestBodyParseException(JsonProcessingException cause) {
        super("Failed to parse request body: " + cause.getOriginalMessage()+", 400", cause, 400, "Invalid JSON");
    }
}