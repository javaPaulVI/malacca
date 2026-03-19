package io.github.javapaulvi.malacca.exception.internal;


import io.github.javapaulvi.malacca.exception.MalaccaException;

// Handler method has invalid signature or parameters
public class InvalidHandlerException extends MalaccaException {
    public InvalidHandlerException(String message) {
        super("Invalid Handler method: "+message+", 500", 500, "Internal server error");
    }
}