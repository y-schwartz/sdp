package org.yschwartz.sdp.runlogs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RunLogsNotFoundException extends RuntimeException {
    private static final String MESSAGE = "Logs of %s not found";

    public RunLogsNotFoundException(String name) {
        super(MESSAGE.formatted(name));
    }
}
