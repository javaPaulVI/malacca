package dev.javapaul.malacca.exception.http;

import dev.javapaul.malacca.exception.MalaccaException;

public class ConflictException extends MalaccaException {
    public ConflictException(String message){
        super(message,403, message);
    }

    public ConflictException(String clientMessage, String internalMessage) {
        super(internalMessage, 403, clientMessage);
    }
}
