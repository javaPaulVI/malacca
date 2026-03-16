package dev.javapaul.malacca.exception.internal;

import dev.javapaul.malacca.exception.MalaccaException;

// Generic exception for internal utility errors
public class MalaccaInternalException extends MalaccaException {
    public MalaccaInternalException(String message) {
        super("Internal Exception in the Malacca framework: "+message+", 500", 500, "Internal server error");
    }

    public MalaccaInternalException(String message, Throwable cause) {
        super("Internal Exception in the Malacca framework: "+message+": "+cause.getMessage()+", ", cause);
    }
}