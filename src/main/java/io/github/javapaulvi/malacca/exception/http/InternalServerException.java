package io.github.javapaulvi.malacca.exception.http;

import io.github.javapaulvi.malacca.exception.MalaccaException;

public class InternalServerException extends MalaccaException {
    public InternalServerException(String message){
        super(message,500, message);
    }

    public InternalServerException(String clientMessage, String internalMessage) {
        super(internalMessage, 500, clientMessage);
    }
}
