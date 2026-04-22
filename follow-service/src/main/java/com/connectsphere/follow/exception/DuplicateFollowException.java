package com.connectsphere.follow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to follow someone they already follow.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateFollowException extends RuntimeException {

    public DuplicateFollowException(String message) {
        super(message);
    }
}