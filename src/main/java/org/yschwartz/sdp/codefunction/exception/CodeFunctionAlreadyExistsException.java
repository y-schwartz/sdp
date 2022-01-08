package org.yschwartz.sdp.codefunction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CodeFunctionAlreadyExistsException extends RuntimeException {
    private static final String MESSAGE = "Code function with name %s already exists";

    public CodeFunctionAlreadyExistsException(String functionName) {
        super(MESSAGE.formatted(functionName));
    }
}
