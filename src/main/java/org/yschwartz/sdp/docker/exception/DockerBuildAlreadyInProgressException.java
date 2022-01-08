package org.yschwartz.sdp.docker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DockerBuildAlreadyInProgressException extends BaseDockerException {
    private static final String MESSAGE = "Build of code function: %s is already is progress";

    public DockerBuildAlreadyInProgressException(String functionName) {
        super(MESSAGE, functionName);
    }
}
