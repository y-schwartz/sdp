package org.yschwartz.sdp.docker.exception;

public class DockerStopException extends BaseDockerException {
    private static final String MESSAGE = "Failed to stop container: %s";

    public DockerStopException(String containerName, Throwable cause) {
        super(cause, MESSAGE, containerName);
    }
}
