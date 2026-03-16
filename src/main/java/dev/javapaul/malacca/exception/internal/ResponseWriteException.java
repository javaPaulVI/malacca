package dev.javapaul.malacca.exception.internal;

import dev.javapaul.malacca.exception.MalaccaException;

import java.io.IOException;

// Error while writing response to HttpExchange
public class ResponseWriteException extends MalaccaException {
    public ResponseWriteException(IOException cause) {
        super("Failed to write response: " + cause.getMessage()+", 500", cause, 500, "Internal server error");
    }
}
