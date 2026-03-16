package dev.javapaul.malacca.exception;


/**
 * Base exception for all Malacca framework errors.
 * Can optionally carry an HTTP status code and client message.
 */
public class MalaccaException extends RuntimeException {
    private final int statusCode;
    private final String clientMessage;
    private final String internalMessage;

    public MalaccaException(String message) {
        super(message);
        this.statusCode = 500;
        this.internalMessage = message;
        this.clientMessage = message;
    }

    public MalaccaException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.clientMessage = message;
        this.internalMessage = message;

    }

    public MalaccaException(String message, int statusCode, String clientMessage) {
        super(message);
        this.statusCode = statusCode;
        this.clientMessage = clientMessage;
        this.internalMessage = message;
    }

    public MalaccaException(String message, Throwable cause, int statusCode, String clientMessage) {
        super(message, cause);
        this.statusCode = statusCode;
        this.clientMessage = clientMessage;
        this.internalMessage = message;
    }

    public int statusCode() { return statusCode; }
    public String clientMessage() { return clientMessage; }
    public String internalMessage() { return internalMessage; }
}