package io.github.javapaulvi.malacca.exception.internal;

import io.github.javapaulvi.malacca.exception.MalaccaException;

public class ServerStartException extends MalaccaException {
    public ServerStartException(Throwable cause) {
        super("Failed to start Malacca server: " + cause.getMessage() +", 500", 500, "Internal server error");
    }
}