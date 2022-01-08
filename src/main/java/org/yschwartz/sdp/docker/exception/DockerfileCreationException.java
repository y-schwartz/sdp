package org.yschwartz.sdp.docker.exception;

public class DockerfileCreationException extends BaseDockerException {
    private static final String MESSAGE = "Failed to create a Dockerfile for code function: %s ";

    public DockerfileCreationException(String functionName, Throwable cause) {
        super(cause, MESSAGE, functionName);
    }
}
