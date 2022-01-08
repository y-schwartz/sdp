package org.yschwartz.sdp.docker.exception;

public class DockerLogsException extends BaseDockerException {
    private static final String MESSAGE = "Failed to handle logs of container: %s";

    public DockerLogsException(String containerName, Throwable cause) {
        super(cause, MESSAGE, containerName);
    }
}
