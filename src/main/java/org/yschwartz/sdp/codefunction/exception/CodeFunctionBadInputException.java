package org.yschwartz.sdp.codefunction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CodeFunctionBadInputException extends RuntimeException {
    private static final String MESSAGE = "Invalid value for field %s";

    public CodeFunctionBadInputException(String fieldName) {
        super(MESSAGE.formatted(fieldName));
    }
}
