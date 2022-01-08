package org.yschwartz.sdp.codefunction.service;

import static org.yschwartz.sdp.common.util.FunctionalUtil.getOrCreate;
import static org.yschwartz.sdp.common.util.StringUtils.createContainerName;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.yschwartz.sdp.codefunction.exception.CodeFunctionNotFoundException;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.codefunction.model.DeploymentDetails;
import org.yschwartz.sdp.codefunction.repository.CodeFunctionRepository;
import org.yschwartz.sdp.common.service.FileService;
import org.yschwartz.sdp.docker.service.DockerBuildService;
import org.yschwartz.sdp.docker.service.DockerRunService;
import org.yschwartz.sdp.rundetails.model.RunDetails;
import org.yschwartz.sdp.rundetails.service.RunDetailsService;
import org.yschwartz.sdp.schedule.service.ScheduleService;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
@Log4j2
public class CodeFunctionService {
    private final CodeFunctionRepository codeFunctionRepository;
    private final DockerRunService dockerRunService;
    private final DockerBuildService dockerBuildService;
    private final FunctionValidationService functionValidationService;
    private final RunDetailsService runDetailsService;
    private final FunctionDeploymentService functionDeploymentService;
    private final FileService fileService;
    private final ScheduleService scheduleService;

    public CodeFunctionService(CodeFunctionRepository codeFunctionRepository, DockerRunService dockerRunService, DockerBuildService dockerBuildService, FunctionValidationService functionValidationService, RunDetailsService runDetailsService, FunctionDeploymentService functionDeploymentService, FileService fileService, ScheduleService scheduleService) {
        this.codeFunctionRepository = codeFunctionRepository;
        this.dockerRunService = dockerRunService;
        this.dockerBuildService = dockerBuildService;
        this.functionValidationService = functionValidationService;
        this.runDetailsService = runDetailsService;
        this.functionDeploymentService = functionDeploymentService;
        this.fileService = fileService;
        this.scheduleService = scheduleService;
    }

    public Mono<CodeFunction> getCodeFunction(String functionName) {
        return codeFunctionRepository.findById(functionName)
                .switchIfEmpty(Mono.error(() -> new CodeFunctionNotFoundException(functionName)))
                .map(this::populateTransientFields);
    }

    public Flux<CodeFunction> getAllCodeFunctions() {
        return codeFunctionRepository.findAll().map(this::populateTransientFields);
    }

    public Mono<CodeFunction> createCodeFunction(CodeFunction codeFunction) {
        return functionValidationService.validateAndCreate(codeFunction, getCodeFunction(codeFunction.getName()))
                .flatMap(codeFunctionRepository::save)
                .doOnSuccess(functionDeploymentService::createMainFile)
                .doOnSuccess(function -> dockerBuildService.build(function, true).subscribe())
                .doOnSuccess(function -> scheduleService.createOrUpdateTask(function.getName(), function.getSchedule(), () -> triggerCodeFunction(function.getName()).subscribe()))
                .map(this::populateTransientFields);
    }

    public Mono<CodeFunction> updateCodeFunction(CodeFunction codeFunction) {
        return functionValidationService.validateAndUpdate(codeFunction, getCodeFunction(codeFunction.getName()))
                .flatMap(codeFunctionRepository::save)
                .doOnSuccess(function -> dockerBuildService.build(function, false).subscribe())
                .doOnSuccess(function -> scheduleService.createOrUpdateTask(function.getName(), function.getSchedule(), () -> triggerCodeFunction(function.getName()).subscribe()))
                .map(this::populateTransientFields);
    }

    public Mono<RunDetails> triggerCodeFunction(String functionName) {
        var containerName = createContainerName(functionName);
        var codeFunctionRef = new AtomicReference<CodeFunction>();
        return getCodeFunction(functionName)
                .doOnSuccess(codeFunctionRef::set)
                .flatMap(x -> runDetailsService.saveInProgressDetails(containerName, functionName))
                .doOnSuccess(details -> dockerRunService.run(codeFunctionRef.get(), containerName)
                        .flatMap(i -> runDetailsService.saveSuccessDetails(details, i))
                        .onErrorResume(e -> runDetailsService.saveFailureDetails(details, e.getMessage()))
                        .subscribe());
    }

    public Mono<CodeFunction> deployCodeFunction(String functionName) {
        return getCodeFunction(functionName)
                .doOnSuccess(x -> functionDeploymentService.deploy(functionName))
                .map(CodeFunctionService::updateDeploymentDetails)
                .flatMap(codeFunctionRepository::save)
                .map(this::populateTransientFields);
    }

    public Mono<String> deleteCodeFunction(String functionName) {
        return getCodeFunction(functionName)
                .flatMap(codeFunctionRepository::delete)
                .doOnSuccess(function -> scheduleService.deleteTask(functionName))
                .doOnSuccess(x -> fileService.deleteAll(functionName))
                .doOnSuccess(x -> runDetailsService.deleteAllRunDetails(functionName).subscribe())
                .then(Mono.just(functionName));
    }

    public Flux<String> deleteAll() {
        return getAllCodeFunctions()
                .map(CodeFunction::getName)
                .flatMap(this::deleteCodeFunction);
    }

    private CodeFunction populateTransientFields(CodeFunction codeFunction) {
        var deploymentDetails = getOrCreate(codeFunction, CodeFunction::getDeploymentDetails, CodeFunction::setDeploymentDetails, DeploymentDetails::new);
        var functionName = codeFunction.getName();
        deploymentDetails.setBuildStatus(dockerBuildService.getStatus(functionName));
        deploymentDetails.setPendingChanges(functionDeploymentService.hasChanges(functionName));
        return codeFunction;
    }

    private static CodeFunction updateDeploymentDetails(CodeFunction codeFunction) {
        var deploymentDetails = getOrCreate(codeFunction, CodeFunction::getDeploymentDetails, CodeFunction::setDeploymentDetails, DeploymentDetails::new);
        deploymentDetails.setLastDeployed(LocalDateTime.now());
        return codeFunction;
    }
}
