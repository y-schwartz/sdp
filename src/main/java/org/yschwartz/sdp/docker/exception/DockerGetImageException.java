package org.yschwartz.sdp.docker.exception;

public class DockerGetImageException extends BaseDockerException {
    private static final String MESSAGE = "Failed to get image : %s";

    public DockerGetImageException(String containerName, Throwable cause) {
        super(cause, MESSAGE, containerName);
    }
}
