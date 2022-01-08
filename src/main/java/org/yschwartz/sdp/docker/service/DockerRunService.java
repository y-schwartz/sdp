package org.yschwartz.sdp.docker.service;

import static org.yschwartz.sdp.common.config.Constants.NEW_LINE;
import static org.yschwartz.sdp.common.service.FileService.FileType.LOGS;
import static org.yschwartz.sdp.common.util.StringUtils.getLogFileName;
import static org.yschwartz.sdp.common.util.StringUtils.getSeparator;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.common.service.FileService;
import org.yschwartz.sdp.docker.client.ReactiveDockerClient;
import org.yschwartz.sdp.docker.exception.DockerRunException;
import org.yschwartz.sdp.docker.mapper.DockerInputMapper;
import org.yschwartz.try_utils.model.Try;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Log4j2
public class DockerRunService {
    private static final String EXITED_STATE = "exited";
    private static final String RUNNING_STATE = "running";
    private static final String STILL_RUNNING_MESSAGE = "still_running";

    private final ReactiveDockerClient reactiveDockerClient;
    private final DockerBuildService dockerBuildService;
    private final DockerInputMapper mapper;
    private final FileService fileService;

    @Value("${docker.run.backoff:50}")
    private int minBackoff;
    @Value("${logs.separator.char:-}")
    private char separatorChar;
    @Value("${logs.separator.length:50}")
    private int separatorLength;
    @Value("${docker.run.remove:true}")
    private boolean remove;

    public DockerRunService(ReactiveDockerClient reactiveDockerClient, DockerBuildService dockerBuildService, DockerInputMapper mapper, FileService fileService) {
        this.reactiveDockerClient = reactiveDockerClient;
        this.dockerBuildService = dockerBuildService;
        this.mapper = mapper;
        this.fileService = fileService;
    }

    public Mono<Integer> run(CodeFunction codeFunction, String containerName) {
        var logFile = fileService.getFile(LOGS, codeFunction.getName(), getLogFileName(containerName));
        var createCommand = mapper.mapCodeFunctionToCreateCommandInput(codeFunction, containerName);
        var start = new AtomicReference<Instant>();
        return Mono.fromRunnable(() -> start.set(Instant.now()))
                .then(dockerBuildService.build(codeFunction, false))
                .then(reactiveDockerClient.create(createCommand, containerName))
                .onErrorResume(e -> e.getCause() instanceof NotFoundException,
                        e -> dockerBuildService.build(codeFunction, true).then(reactiveDockerClient.create(createCommand, containerName)))
                .then(reactiveDockerClient.start(containerName))
                .doOnSuccess(x -> logStarted(logFile, start.get()))
                .doOnSuccess(x -> reactiveDockerClient.logs(containerName, frame -> logToFile(logFile, frame.toString())).subscribe())
                .then(waitForComplete(containerName, codeFunction.getRunTimeout(), start))
                .doOnSuccess(x -> Optional.of(remove).filter(y -> y).ifPresent(y -> reactiveDockerClient.remove(containerName).subscribe()))
                .doOnSuccess(code -> logCompleted(logFile, start.get(), code))
                .onErrorMap(e -> new DockerRunException(codeFunction.getName(), e));
    }

    private Mono<Integer> waitForComplete(String containerName, long timeout, AtomicReference<Instant> start) {
        return reactiveDockerClient.get(containerName)
                .map(container -> waitForComplete(container, containerName, timeout, start))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(minBackoff)).filter(e -> STILL_RUNNING_MESSAGE.equals(e.getMessage())));
    }

    private Integer waitForComplete(Container container, String containerName, long timeout, AtomicReference<Instant> start) {
        var state = container.getState();
        if (EXITED_STATE.equals(state))
            return getExitCode(container);
        if (!RUNNING_STATE.equals(state))
            throw new RuntimeException("Unexpected container state %s for container: %s" .formatted(state, containerName));
        if (getDuration(start.get()).toMinutes() > timeout)
            reactiveDockerClient.stop(containerName).subscribe();
        throw new RuntimeException(STILL_RUNNING_MESSAGE);
    }

    private Integer getExitCode(Container container) {
        return Optional.of(container)
                .map(Container::getStatus)
                .map(s -> StringUtils.substringBetween(s, "(", ")"))
                .filter(StringUtils::isNumeric)
                .map(Integer::valueOf)
                .orElse(-1);
    }

    private void logStarted(File logFile, Instant start) {
        logToFile(logFile, "Started function in %sms" .formatted(getDuration(start).toMillis()));
        logToFile(logFile, getSeparator(separatorLength, separatorChar));
    }

    private void logCompleted(File logFile, Instant start, Integer code) {
        logToFile(logFile, getSeparator(separatorLength, separatorChar));
        logToFile(logFile, "Function completed after %sms with exit code: %s" .formatted(getDuration(start).toMillis(), code));
    }

    private void logToFile(File logFile, String data) {
        Try.of(() -> FileUtils.writeStringToFile(logFile, data + NEW_LINE, Charset.defaultCharset(), true))
                .catchAny()
                .thenDo(e -> log.error("Failed to write to log file: {}. Data: {}", logFile, data, e))
                .execute();
    }

    private static Duration getDuration(Instant before) {
        return Duration.between(before, Instant.now());
    }
}
