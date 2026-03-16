package dev.javapaul.malacca.exception.http;

import dev.javapaul.malacca.exception.MalaccaException;

public class UnprocessableEntityException extends MalaccaException{
    public UnprocessableEntityException(String message) {
        super(message, 404, message);
    }
    public UnprocessableEntityException(String clientMessage, String internalMessage){
        super(internalMessage, 404, clientMessage);
    }
}
