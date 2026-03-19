package io.github.javapaulvi.malacca.exception.http;

import io.github.javapaulvi.malacca.exception.MalaccaException;

public class NotFoundException extends MalaccaException {
    public NotFoundException(String message) {
        super(message, 404, message);
    }
    public NotFoundException(String clientMessage, String internalMessage){
        super(internalMessage, 404, clientMessage);
    }
}
