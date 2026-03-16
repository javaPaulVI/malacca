package dev.javapaul.malacca.exception.http;

import dev.javapaul.malacca.exception.MalaccaException;

public class UnauthorizedException extends MalaccaException {
    public UnauthorizedException(String message) {
        super(message, 401, message);
    }
    public UnauthorizedException(String clientMessage, String internalMessage){
        super(internalMessage, 401, clientMessage);

    }
}
