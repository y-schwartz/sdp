package org.yschwartz.sdp.codetype.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CodeTypeNotFoundException extends RuntimeException {
    private static final String MESSAGE = "Code type %s not found";

    public CodeTypeNotFoundException(String codeTypeName) {
        super(MESSAGE.formatted(codeTypeName));
    }
}
