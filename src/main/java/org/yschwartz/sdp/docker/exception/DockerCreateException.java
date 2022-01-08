package org.yschwartz.sdp.docker.exception;

public class DockerCreateException extends BaseDockerException {
    private static final String MESSAGE = "Failed to create container: %s";

    public DockerCreateException(String containerName, Throwable cause) {
        super(cause, MESSAGE, containerName);
    }
}
