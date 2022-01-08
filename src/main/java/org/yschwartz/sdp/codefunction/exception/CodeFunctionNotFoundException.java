package org.yschwartz.sdp.codefunction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CodeFunctionNotFoundException extends RuntimeException {
    private static final String MESSAGE = "Code function %s not found";

    public CodeFunctionNotFoundException(String functionName) {
        super(MESSAGE.formatted(functionName));
    }
}
