package org.yschwartz.sdp.docker.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.docker.client.ReactiveDockerClient;
import org.yschwartz.sdp.docker.exception.DockerBuildAlreadyInProgressException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class DockerBuildService {
    private final ReactiveDockerClient reactiveDockerClient;
    private final DockerfileService dockerfileService;

    private final ConcurrentMap<String, Status> functionStatuses = new ConcurrentHashMap<>();
    private final Map<String, Integer> functionHashes = new HashMap<>();

    @Value("${docker.build.retries:10}")
    private int retries;
    @Value("${docker.build.backoff:50}")
    private int delay;

    public enum Status {IN_PROGRESS, SUCCESS, FAILURE}

    public DockerBuildService(ReactiveDockerClient reactiveDockerClient, DockerfileService dockerfileService) {
        this.reactiveDockerClient = reactiveDockerClient;
        this.dockerfileService = dockerfileService;
    }

    public Status getStatus(String functionName) {
        return functionStatuses.get(functionName);
    }

    public Mono<Void> build(CodeFunction codeFunction, boolean force) {
        var functionName = codeFunction.getName();
        if (!force && getStatus(functionName) == Status.SUCCESS && functionHashes.get(functionName) == getHash(codeFunction))
            return Mono.empty();
        return build(codeFunction, functionName).retryWhen(Retry.backoff(retries, Duration.ofMillis(delay))
                .filter(t -> t instanceof DockerBuildAlreadyInProgressException));
    }

    private Mono<Void> build(CodeFunction codeFunction, String functionName) {
        return Mono.fromRunnable(() -> validateNotInProgress(functionName))
                .doOnSuccess(x -> functionHashes.remove(functionName))
                .then(Mono.just(dockerfileService.getDockerfile(codeFunction)))
                .flatMap(dockerfile -> reactiveDockerClient.build(dockerfile, functionName)
                        .doOnError(e -> saveStatus(functionName, Status.IN_PROGRESS, Status.FAILURE))
                        .doOnSuccess(x -> functionHashes.put(functionName, getHash(codeFunction)))
                        .doOnSuccess(x -> saveStatus(functionName, Status.IN_PROGRESS, Status.SUCCESS)));
    }

    private void validateNotInProgress(String functionName) {
        var previousStatus = functionStatuses.get(functionName);
        if (previousStatus == Status.IN_PROGRESS)
            throw new DockerBuildAlreadyInProgressException(functionName);
        if (previousStatus != null && !saveStatus(functionName, previousStatus, Status.IN_PROGRESS))
            throw new DockerBuildAlreadyInProgressException(functionName);
        if (previousStatus == null && functionStatuses.putIfAbsent(functionName, Status.IN_PROGRESS) != null)
            throw new DockerBuildAlreadyInProgressException(functionName);
    }

    private boolean saveStatus(String functionName, Status expectedValue, Status newValue) {
        return functionStatuses.replace(functionName, expectedValue, newValue);
    }

    private static int getHash(CodeFunction codeFunction) {
        return Objects.hash(codeFunction.getCodeTypeName(), codeFunction.getDependencies(), codeFunction.getAdditionalCommands());
    }
}
