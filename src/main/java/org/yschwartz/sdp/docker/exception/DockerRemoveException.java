package org.yschwartz.sdp.docker.exception;

public class DockerRemoveException extends BaseDockerException {
    private static final String MESSAGE = "Failed to remove container: %s";

    public DockerRemoveException(String containerName, Throwable cause) {
        super(cause, MESSAGE, containerName);
    }
}
