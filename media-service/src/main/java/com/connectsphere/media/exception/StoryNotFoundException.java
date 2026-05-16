package com.connectsphere.media.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StoryNotFoundException extends RuntimeException {

    public StoryNotFoundException(String message) {
        super(message);
    }
}