package dev.javapaul.malacca.exception.http;

import dev.javapaul.malacca.exception.MalaccaException;

public class UnprocessableEntityException extends MalaccaException{
    public UnprocessableEntityException(String message) {
        super(message, 422, message);
    }
    public UnprocessableEntityException(String clientMessage, String internalMessage){
        super(internalMessage, 422, clientMessage);
    }
}
