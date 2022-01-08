package org.yschwartz.sdp.docker.exception;

public class DockerBuildException extends BaseDockerException {
    private static final String MESSAGE = "Failed to build code function: %s";

    public DockerBuildException(String functionName, Throwable cause) {
        super(cause, MESSAGE, functionName);
    }
}
