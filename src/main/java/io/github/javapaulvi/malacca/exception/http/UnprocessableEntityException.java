package io.github.javapaulvi.malacca.exception.http;

import io.github.javapaulvi.malacca.exception.MalaccaException;

public class UnprocessableEntityException extends MalaccaException{
    public UnprocessableEntityException(String message) {
        super(message, 422, message);
    }
    public UnprocessableEntityException(String clientMessage, String internalMessage){
        super(internalMessage, 422, clientMessage);
    }
}
