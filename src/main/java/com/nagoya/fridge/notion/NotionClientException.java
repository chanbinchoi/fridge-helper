package com.nagoya.fridge.notion;

public class NotionClientException extends RuntimeException {

    public NotionClientException(String message) {
        super(message);
    }

    public NotionClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
