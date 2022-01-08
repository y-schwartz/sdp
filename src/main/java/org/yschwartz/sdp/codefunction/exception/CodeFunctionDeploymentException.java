package org.yschwartz.sdp.codefunction.exception;

public class CodeFunctionDeploymentException extends RuntimeException {
    private static final String MESSAGE = "Failed to deploy code function: %s";

    public CodeFunctionDeploymentException(String functionName, Throwable e) {
        super(MESSAGE.formatted(functionName), e);
    }
}
