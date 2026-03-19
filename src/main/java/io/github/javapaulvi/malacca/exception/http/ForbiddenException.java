package io.github.javapaulvi.malacca.exception.http;

import io.github.javapaulvi.malacca.exception.MalaccaException;

public class ForbiddenException extends MalaccaException {
    public ForbiddenException(String message){
        super(message,403, message);
    }

    public ForbiddenException(String clientMessage, String internalMessage) {
        super(internalMessage, 403, clientMessage);
    }

}
