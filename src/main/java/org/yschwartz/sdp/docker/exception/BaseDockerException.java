package org.yschwartz.sdp.docker.exception;

public abstract class BaseDockerException extends RuntimeException {

    protected BaseDockerException(String message, String... args) {
        super(message.formatted((Object[]) args));
    }

    protected BaseDockerException(Throwable t, String message, String... args) {
        super(message.formatted((Object[]) args), t);
    }
}
