package org.yschwartz.sdp.docker.exception;

public class DockerPullException extends BaseDockerException {
    private static final String MESSAGE = "Failed to pull image: %s";

    public DockerPullException(String image, Throwable cause) {
        super(cause, MESSAGE, image);
    }
}
