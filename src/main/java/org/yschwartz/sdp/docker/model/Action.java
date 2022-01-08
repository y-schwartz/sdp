package org.yschwartz.sdp.docker.model;

import org.yschwartz.sdp.docker.exception.*;

public enum Action {
    PULL, BUILD, CREATE, START, LOGS, GET, STOP, REMOVE, GET_IMAGE;

    public RuntimeException getDockerException(Throwable t, String arg) {
        switch (this) {
            case PULL -> {
                return new DockerPullException(arg, t);
            }
            case BUILD -> {
                return new DockerBuildException(arg, t);
            }
            case CREATE -> {
                return new DockerCreateException(arg, t);
            }
            case START -> {
                return new DockerStartException(arg, t);
            }
            case LOGS -> {
                return new DockerLogsException(arg, t);
            }
            case GET -> {
                return new DockerGetException(arg, t);
            }
            case STOP -> {
                return new DockerStopException(arg, t);
            }
            case REMOVE -> {
                return new DockerRemoveException(arg, t);
            }
            case GET_IMAGE -> {
                return new DockerGetImageException(arg, t);
            }
        }
        return new RuntimeException();
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
