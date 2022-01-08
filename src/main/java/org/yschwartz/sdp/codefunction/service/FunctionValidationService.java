package org.yschwartz.sdp.codefunction.service;

import static org.springframework.scheduling.support.CronExpression.isValidExpression;
import static org.yschwartz.sdp.common.util.FunctionalUtil.getOrCreate;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.codefunction.exception.CodeFunctionAlreadyExistsException;
import org.yschwartz.sdp.codefunction.exception.CodeFunctionBadInputException;
import org.yschwartz.sdp.codefunction.exception.CodeFunctionNotFoundException;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.codetype.service.CodeTypeService;
import org.yschwartz.sdp.schedule.model.BaseSchedule;
import org.yschwartz.sdp.schedule.model.CronSchedule;
import org.yschwartz.sdp.schedule.model.FixedDelaySchedule;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class FunctionValidationService {
    private static final String FUNCTION_NAME_REGEX = "^[-a-z0-9_-]+$";

    private final CodeTypeService codeTypeService;

    @Value("${run.timeout.default:30}")
    private int defaultRunTimeout;

    public FunctionValidationService(CodeTypeService codeTypeService) {
        this.codeTypeService = codeTypeService;
    }

    public Mono<CodeFunction> validateAndCreate(CodeFunction codeFunction, Mono<CodeFunction> functionMono) {
        return validateForCreate(codeFunction)
                .then(validateFunctionNotExists(functionMono, codeFunction.getName()))
                .then(Mono.just(codeFunction))
                .map(this::setNewFunctionFields);
    }

    public Mono<CodeFunction> validateAndUpdate(CodeFunction codeFunction, Mono<CodeFunction> functionMono) {
        return validateForUpdate(codeFunction)
                .then(functionMono)
                .map(old -> setUpdatedFunctionFields(old, codeFunction));
    }

    private Mono<Void> validateForCreate(CodeFunction codeFunction) {
        return validateNotEmpty("name", codeFunction.getName())
                .then(validateField("name", codeFunction.getName().matches(FUNCTION_NAME_REGEX)))
                .then(validateNotEmpty("codeTypeName", codeFunction.getCodeTypeName()))
                .then(validateRunTimeout(codeFunction.getRunTimeout()))
                .then(validateSchedule(codeFunction.getSchedule()))
                .then(validateVolumes(codeFunction.getVolumes()))
                .then(validateLists(codeFunction))
                .then(validateCodeTypeExists(codeFunction.getCodeTypeName()));
    }

    private Mono<Void> validateForUpdate(CodeFunction codeFunction) {
        return validateNotEmpty("name", codeFunction.getName())
                .then(validateSchedule(codeFunction.getSchedule()))
                .then(validateVolumes(codeFunction.getVolumes()))
                .then(validateRunTimeout(codeFunction.getRunTimeout()))
                .then(validateLists(codeFunction))
                .then(validateCodeTypeNullOrExists(codeFunction.getCodeTypeName()));
    }

    private Mono<Void> validateFunctionNotExists(Mono<CodeFunction> functionMono, String functionName) {
        return functionMono.flatMap(x -> Mono.error(() -> new CodeFunctionAlreadyExistsException(functionName)))
                .onErrorResume(CodeFunctionNotFoundException.class, e -> Mono.empty())
                .then();
    }

    private Mono<Void> validateCodeTypeNullOrExists(String codeTypeName) {
        if (codeTypeName == null)
            return Mono.empty();
        return validateCodeTypeExists(codeTypeName);
    }

    private Mono<Void> validateCodeTypeExists(String codeTypeName) {
        return Mono.fromCallable(() -> codeTypeService.getCodeType(codeTypeName)).then();
    }

    private CodeFunction setNewFunctionFields(CodeFunction newFunction) {
        var now = LocalDateTime.now();
        newFunction.setCreationDate(now);
        newFunction.setLastUpdated(now);
        newFunction.setDeploymentDetails(null);
        setOtherFields(newFunction);
        return newFunction;
    }

    private void setOtherFields(CodeFunction codeFunction) {
        getOrCreate(codeFunction, CodeFunction::getDependencies, CodeFunction::setDependencies, LinkedList::new);
        getOrCreate(codeFunction, CodeFunction::getAdditionalCommands, CodeFunction::setAdditionalCommands, LinkedList::new);
        getOrCreate(codeFunction, CodeFunction::getVolumes, CodeFunction::setVolumes, LinkedList::new);
        getOrCreate(codeFunction, CodeFunction::getTags, CodeFunction::setTags, LinkedList::new);
        getOrCreate(codeFunction, CodeFunction::getEnvironmentVariables, CodeFunction::setEnvironmentVariables, LinkedList::new);
        getOrCreate(codeFunction, CodeFunction::getNetworkMode, CodeFunction::setNetworkMode, () -> CodeFunction.NetworkMode.NONE);
        getOrCreate(codeFunction, CodeFunction::getPrivileged, CodeFunction::setPrivileged, () -> false);
        getOrCreate(codeFunction, CodeFunction::getRunTimeout, CodeFunction::setRunTimeout, () -> defaultRunTimeout);
    }

    private static Mono<Void> validateNotNull(String fieldName, Object value) {
        return validateField(fieldName, value != null);
    }

    private static Mono<Void> validateRunTimeout(Integer value) {
        return validateField("runTimeout", value == null || value > 0);
    }

    private static Mono<Void> validateDelay(long value) {
        return validateField("schedule.delay", value > 0);
    }

    private static Mono<Void> validateNotEmpty(String fieldName, String value) {
        return validateField(fieldName, StringUtils.isNotBlank(value));
    }

    private static Mono<Void> validateSchedule(BaseSchedule schedule) {
        if (schedule == null)
            return Mono.empty();
        if (schedule instanceof CronSchedule)
            return validateField("schedule.cron", isValidExpression(((CronSchedule) schedule).getCron()));
        return validateDelay(((FixedDelaySchedule) schedule).getDelay())
                .then(validateNotNull("schedule.timeUnit", ((FixedDelaySchedule) schedule).getTimeUnit()));
    }

    private static Mono<Void> validateVolumes(List<CodeFunction.Volume> volumes) {
        if (volumes == null)
            return Mono.empty();
        return Flux.fromIterable(volumes)
                .flatMap(v -> validateNotEmpty("volumes.containerPath", v.getContainerPath())
                        .then(validateNotEmpty("volumes.hostPath", v.getHostPath())))
                .then();
    }

    private static Mono<Void> validateLists(CodeFunction codeFunction) {
        return validateStringList("dependencies", codeFunction.getDependencies())
                .then(validateStringList("additionalCommands", codeFunction.getAdditionalCommands()))
                .then(validatePairList("tags", codeFunction.getTags()))
                .then(validatePairList("environmentVariables", codeFunction.getEnvironmentVariables()));
    }

    private static Mono<Void> validateStringList(String fieldName, List<String> list) {
        if (list == null)
            return Mono.empty();
        return Flux.fromIterable(list)
                .flatMap(s -> validateNotEmpty(fieldName, s))
                .then();
    }

    private static Mono<Void> validatePairList(String fieldName, List<CodeFunction.KeyValuePair> list) {
        if (list == null)
            return Mono.empty();
        return Flux.fromIterable(list)
                .flatMap(pair -> validateNotNull(fieldName, pair)
                        .then(validateNotEmpty("%s.key".formatted(fieldName), pair.getKey()))
                        .then(validateNotEmpty("%s.value".formatted(fieldName), pair.getValue())))
                .then(validateField("%s".formatted(fieldName), list.size() == list.stream().map(CodeFunction.KeyValuePair::getKey).distinct().count()));
    }

    private static Mono<Void> validateField(String fieldName, Boolean condition) {
        return Mono.fromRunnable(() -> {
            if (!condition)
                throw new CodeFunctionBadInputException(fieldName);
        });
    }

    private static CodeFunction setUpdatedFunctionFields(CodeFunction oldFunction, CodeFunction newFunction) {
        oldFunction.setLastUpdated(LocalDateTime.now());
        setIfNotNull(oldFunction, newFunction.getTags(), CodeFunction::setTags);
        setIfNotNull(oldFunction, newFunction.getCodeTypeName(), CodeFunction::setCodeTypeName);
        setIfNotNull(oldFunction, newFunction.getDependencies(), CodeFunction::setDependencies);
        setIfNotNull(oldFunction, newFunction.getAdditionalCommands(), CodeFunction::setAdditionalCommands);
        setIfNotNull(oldFunction, newFunction.getEnvironmentVariables(), CodeFunction::setEnvironmentVariables);
        setIfNotNull(oldFunction, newFunction.getVolumes(), CodeFunction::setVolumes);
        setIfNotNull(oldFunction, newFunction.getNetworkMode(), CodeFunction::setNetworkMode);
        setIfNotNull(oldFunction, newFunction.getPrivileged(), CodeFunction::setPrivileged);
        setIfNotNull(oldFunction, newFunction.getRunTimeout(), CodeFunction::setRunTimeout);
        setIfNotNull(oldFunction, newFunction.getSchedule(), CodeFunction::setSchedule);
        return oldFunction;
    }

    private static <S, T> void setIfNotNull(S object, T value, BiConsumer<S, T> setMethod) {
        Optional.ofNullable(value).ifPresent(t -> setMethod.accept(object, t));
    }
}
