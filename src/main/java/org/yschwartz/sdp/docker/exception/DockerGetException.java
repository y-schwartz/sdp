package org.yschwartz.sdp.docker.exception;

public class DockerGetException extends BaseDockerException {
    private static final String MESSAGE = "Failed to get container : %s";

    public DockerGetException(String containerName, Throwable cause) {
        super(cause, MESSAGE, containerName);
    }
}
