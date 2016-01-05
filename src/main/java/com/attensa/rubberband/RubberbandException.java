package com.attensa.rubberband;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RubberbandException extends RuntimeException {
    int statusCode;
    String message;

    public RubberbandException(int statusCode, String message, Throwable cause) {
        super(cause);
        this.statusCode = statusCode;
        this.message = message;
    }
}
