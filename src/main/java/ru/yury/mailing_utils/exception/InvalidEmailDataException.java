package ru.yury.mailing_utils.exception;

public class InvalidEmailDataException extends RuntimeException {

    public InvalidEmailDataException(String message) {
        super(message);
    }
}