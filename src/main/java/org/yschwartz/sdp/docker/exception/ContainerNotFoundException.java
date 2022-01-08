package org.yschwartz.sdp.docker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ContainerNotFoundException extends BaseDockerException {
    private static final String MESSAGE = "Container %s not found";

    public ContainerNotFoundException(String containerName) {
        super(MESSAGE, containerName);
    }
}
