package org.yschwartz.sdp.docker.exception;

public class DockerStartException extends BaseDockerException {
    private static final String MESSAGE = "Failed to start container: %s";

    public DockerStartException(String containerName, Throwable cause) {
        super(cause, MESSAGE, containerName);
    }
}
