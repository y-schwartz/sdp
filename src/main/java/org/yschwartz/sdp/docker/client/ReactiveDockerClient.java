package org.yschwartz.sdp.docker.client;

import static org.yschwartz.sdp.common.config.Constants.IMAGE_TAG;

import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.docker.exception.ContainerNotFoundException;
import org.yschwartz.sdp.docker.model.Action;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.AsyncDockerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.SyncDockerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.DockerObject;
import com.github.dockerjava.api.model.Frame;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@Service
@Log4j2
public class ReactiveDockerClient {
    private final DockerClient dockerClient;

    @Value("${docker.client.retries:3}")
    private int retries;

    public ReactiveDockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public Mono<Void> pull(String image) {
        return exec(dockerClient.pullImageCmd(image), image, Action.PULL);
    }

    public Mono<Void> build(File dockerfile, String functionName) {
        return exec(dockerClient.buildImageCmd(dockerfile).withTags(Set.of(IMAGE_TAG.formatted(functionName))), functionName, Action.BUILD)
                .then(validateImageCreated(functionName))
                .then();
    }

    public Mono<Void> create(CreateContainerCmd command, String containerName) {
        return exec(command, containerName, Action.CREATE);
    }

    public Mono<Void> start(String containerName) {
        return exec(dockerClient.startContainerCmd(containerName), containerName, Action.START);
    }

    public Mono<Void> logs(String containerName, Consumer<Frame> logHandler) {
        return exec(dockerClient.logContainerCmd(containerName).withStdOut(true).withStdErr(true).withTimestamps(true).withFollowStream(true).withTailAll(),
                containerName,
                Action.LOGS,
                x -> null,
                logHandler);
    }

    public Mono<Container> get(String containerName) {
        return exec(dockerClient.listContainersCmd().withNameFilter(List.of(containerName)).withShowAll(true),
                containerName,
                Action.GET,
                list -> list.stream().findFirst().orElseThrow(() -> new ContainerNotFoundException(containerName)));
    }

    public Mono<Void> stop(String containerName) {
        return exec(dockerClient.stopContainerCmd(containerName), containerName, Action.STOP);
    }

    public Mono<Void> remove(String containerName) {
        return exec(dockerClient.removeContainerCmd(containerName), containerName, Action.REMOVE);
    }

    private Mono<Void> validateImageCreated(String functionName) {
        return exec(dockerClient.inspectImageCmd(IMAGE_TAG.formatted(functionName)),
                functionName,
                Action.GET_IMAGE);
    }

    private <A extends DockerObject> Mono<Void> exec(AsyncDockerCmd<?, A> command, String identifier, Action action) {
        return exec(command, identifier, action, x -> null, null);
    }

    private <A extends DockerObject, R> Mono<R> exec(AsyncDockerCmd<?, A> command, String identifier, Action action, Function<A, R> returnValueMapper, Consumer<A> onNextHook) {
        Mono<A> mono = Mono.create(sink -> command.exec(new Callback<>(sink, identifier, action, onNextHook)));
        return mono.retry(retries).mapNotNull(returnValueMapper);
    }

    private <A> Mono<Void> exec(SyncDockerCmd<A> command, String identifier, Action action) {
        return exec(command, identifier, action, x -> null);
    }

    private <A, R> Mono<R> exec(SyncDockerCmd<A> command, String identifier, Action action, Function<A, R> returnValueMapper) {
        return Mono.empty()
                .doOnSuccess(x -> logStarted(action, identifier))
                .then(Mono.fromCallable(command::exec))
                .doOnSuccess(response -> logResponse(action, identifier, response))
                .doOnSuccess(x -> logCompleted(action, identifier))
                .retry(retries)
                .mapNotNull(returnValueMapper)
                .onErrorMap(e -> action.getDockerException(e, identifier))
                .doOnError(e -> logFailed(action, identifier, e));
    }

    private void logStarted(Action action, String identifier) {
        log.info("Starting {}: {}", action, identifier);
    }

    private void logCompleted(Action action, String identifier) {
        log.info("Completed {}: {}", action, identifier);
    }

    private void logFailed(Action action, String identifier, Throwable e) {
        log.error("Failed {}: {}", action, identifier, e);
    }

    private void logResponse(Action action, String identifier, Object response) {
        if (log.isDebugEnabled())
            log.debug("{}: {}, Response: {}", action, identifier, response);
    }

    private class Callback<A extends DockerObject> extends ResultCallback.Adapter<A> {
        private final MonoSink<A> sink;
        private final String identifier;
        private final Action action;
        private final Consumer<A> onNextHook;
        private A response;

        private Callback(MonoSink<A> sink, String identifier, Action action, Consumer<A> onNextHook) {
            this.sink = sink;
            this.identifier = identifier;
            this.action = action;
            this.onNextHook = onNextHook;
        }

        @Override
        public void onStart(Closeable stream) {
            logStarted(action, identifier);
            super.onStart(stream);
        }

        @Override
        public void onNext(A response) {
            this.response = response;
            Optional.ofNullable(onNextHook).ifPresent(consumer -> consumer.accept(response));
            logResponse(action, identifier, response);
            super.onNext(response);
        }

        @Override
        public void onError(Throwable throwable) {
            var dockerException = action.getDockerException(throwable, identifier);
            sink.error(dockerException);
            logFailed(action, identifier, dockerException);
            super.onError(dockerException);
        }

        @Override
        public void onComplete() {
            sink.success(response);
            logCompleted(action, identifier);
            super.onComplete();
        }
    }
}
