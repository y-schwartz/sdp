package org.yschwartz.sdp.docker.exception;

public class DockerRunException extends BaseDockerException {
    private static final String MESSAGE = "Failed to run code function: %s";

    public DockerRunException(String functionName, Throwable cause) {
        super(cause, MESSAGE, functionName);
    }
}
