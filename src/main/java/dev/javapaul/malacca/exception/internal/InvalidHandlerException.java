package dev.javapaul.malacca.exception.internal;


import dev.javapaul.malacca.exception.MalaccaException;

// Handler method has invalid signature or parameters
public class InvalidHandlerException extends MalaccaException {
    public InvalidHandlerException(String message) {
        super("Invalid Handler method: "+message+", 500", 500, "Internal server error");
    }
}