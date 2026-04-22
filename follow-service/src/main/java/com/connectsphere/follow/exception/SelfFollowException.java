package com.connectsphere.follow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to follow themselves.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SelfFollowException extends RuntimeException {

    public SelfFollowException(String message) {
        super(message);
    }
}