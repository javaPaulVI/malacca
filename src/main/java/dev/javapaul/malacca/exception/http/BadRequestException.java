package dev.javapaul.malacca.exception.http;

import dev.javapaul.malacca.exception.MalaccaException;

public class BadRequestException extends MalaccaException {
    public BadRequestException(String message) {
        super(message, 400, message);
    }
    public BadRequestException(String clientMessage, String internalMessage){
        super(internalMessage, 400, clientMessage);

    }
}
