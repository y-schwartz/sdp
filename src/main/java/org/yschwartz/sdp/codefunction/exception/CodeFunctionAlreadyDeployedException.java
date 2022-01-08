package org.yschwartz.sdp.codefunction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CodeFunctionAlreadyDeployedException extends RuntimeException {
    private static final String MESSAGE = "Code function %s already deployed";

    public CodeFunctionAlreadyDeployedException(String functionName) {
        super(MESSAGE.formatted(functionName));
    }
}
