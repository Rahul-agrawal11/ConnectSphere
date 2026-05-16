package com.connectsphere.like.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to react to a target they have already reacted to.
 * The correct flow for changing a reaction is PUT /api/v1/likes/change.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateReactionException extends RuntimeException {

    public DuplicateReactionException(String message) {
        super(message);
    }
}