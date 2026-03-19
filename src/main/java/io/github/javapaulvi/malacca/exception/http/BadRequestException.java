package io.github.javapaulvi.malacca.exception.http;

import io.github.javapaulvi.malacca.exception.MalaccaException;

public class BadRequestException extends MalaccaException {
    public BadRequestException(String message) {
        super(message, 400, message);
    }
    public BadRequestException(String clientMessage, String internalMessage){
        super(internalMessage, 400, clientMessage);

    }
}
