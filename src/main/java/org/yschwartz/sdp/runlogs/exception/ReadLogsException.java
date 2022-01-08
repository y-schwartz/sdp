package org.yschwartz.sdp.runlogs.exception;

public class ReadLogsException extends RuntimeException {
    private static final String MESSAGE = "Failed to read logs of container: %s";

    public ReadLogsException(String containerName, Throwable e) {
        super(MESSAGE.formatted(containerName), e);
    }
}
