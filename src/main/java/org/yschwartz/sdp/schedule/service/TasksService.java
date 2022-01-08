package org.yschwartz.sdp.schedule.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.codefunction.repository.CodeFunctionRepository;
import org.yschwartz.sdp.codefunction.service.CodeFunctionService;
import org.yschwartz.sdp.codetype.model.CodeType;
import org.yschwartz.sdp.codetype.service.CodeTypeService;
import org.yschwartz.sdp.docker.client.ReactiveDockerClient;
import org.yschwartz.sdp.docker.service.DockerBuildService;
import org.yschwartz.sdp.rundetails.repository.RunDetailsRepository;
import org.yschwartz.sdp.runlogs.service.RunLogsService;
import org.yschwartz.sdp.schedule.model.BaseSchedule;
import org.yschwartz.sdp.schedule.model.CronSchedule;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Log4j2
public class TasksService {
    private static final String UPDATE_IMAGES_TASK_ID = "update-images-task";
    private static final String DELETE_DETAILS_TASK_ID = "delete-old-run-details-task";
    private static final String DELETE_LOGS_TASK_ID = "delete-old-logs-task";

    private final CodeTypeService codeTypeService;
    private final ReactiveDockerClient reactiveDockerClient;
    private final DockerBuildService dockerBuildService;
    private final CodeFunctionRepository codeFunctionRepository;
    private final ScheduleService scheduleService;
    private final CodeFunctionService codeFunctionService;
    private final RunDetailsRepository runDetailsRepository;
    private final RunLogsService runLogsService;

    @Value("${docker.image.update.cron:0 0 4 * * *}")
    private String updateImagesCron;

    @Value("${details.cleanup.cron:0 0 4 * * 0}")
    private String deleteRunDetailsCron;
    @Value("${details.cleanup.retention:30}")
    private int detailsDaysToKeep;

    @Value("${logs.cleanup.cron:0 0 4 * * 0}")
    private String deleteLogsCron;
    @Value("${logs.cleanup.retention:30}")
    private int logsDaysToKeep;

    public TasksService(CodeTypeService codeTypeService, ReactiveDockerClient reactiveDockerClient, DockerBuildService dockerBuildService, CodeFunctionRepository codeFunctionRepository, ScheduleService scheduleService, CodeFunctionService codeFunctionService, RunDetailsRepository runDetailsRepository, RunLogsService runLogsService) {
        this.codeTypeService = codeTypeService;
        this.reactiveDockerClient = reactiveDockerClient;
        this.dockerBuildService = dockerBuildService;
        this.codeFunctionRepository = codeFunctionRepository;
        this.scheduleService = scheduleService;
        this.codeFunctionService = codeFunctionService;
        this.runDetailsRepository = runDetailsRepository;
        this.runLogsService = runLogsService;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void startup() {
        scheduleService.deleteAll();
        updateImages().doOnComplete(this::scheduleFunctions).subscribe();
        scheduleService.createOrUpdateTask(UPDATE_IMAGES_TASK_ID, createCronSchedule(updateImagesCron), () -> updateImages().subscribe());
        scheduleService.createOrUpdateTask(DELETE_DETAILS_TASK_ID, createCronSchedule(deleteRunDetailsCron), this::deleteOldRunDetails);
        scheduleService.createOrUpdateTask(DELETE_LOGS_TASK_ID, createCronSchedule(deleteLogsCron), this::deleteOldLogs);
    }

    private Flux<CodeFunction> updateImages() {
        log.info("Updating images");
        return codeTypeService.getAll()
                .doOnNext(type -> log.debug("Updating code type: {}", type.getName()))
                .map(type -> reactiveDockerClient.pull(type.getFrom()).then(Mono.just(type)))
                .flatMap(Mono::flux)
                .map(CodeType::getName)
                .flatMap(codeFunctionRepository::findAllByCodeTypeName)
                .flatMap(function -> dockerBuildService.build(function, true).then(Mono.just(function)))
                .doOnNext(function -> log.debug("Updated function: {}", function.getName()))
                .doOnError(e -> log.error("Failed to update function", e))
                .doOnComplete(() -> log.info("Updated images"));
    }

    private void deleteOldRunDetails() {
        var before = LocalDateTime.now().minusDays(detailsDaysToKeep);
        log.info("Deleting run details older than {}", before);
        runDetailsRepository.deleteAllByEndTimeBefore(before)
                .doOnError(e -> log.error("Failed to delete run details", e))
                .doOnSuccess(x -> log.info("Deleted run details older than {}", before))
                .subscribe();
    }

    private void deleteOldLogs() {
        var before = LocalDateTime.now().minusDays(logsDaysToKeep);
        log.info("Deleting logs older than {}", before);
        runLogsService.deleteByDateBefore(before);
        log.info("Deleted logs older than {}", before);
    }

    private void scheduleFunctions() {
        codeFunctionRepository.findAll()
                .filter(function -> Optional.ofNullable(function.getSchedule()).filter(BaseSchedule::isOn).isPresent())
                .doOnNext(function -> scheduleService.createOrUpdateTask(function.getName(), function.getSchedule(), () -> codeFunctionService.triggerCodeFunction(function.getName()).subscribe()))
                .subscribe();
    }

    private static CronSchedule createCronSchedule(String cron) {
        var schedule = new CronSchedule();
        schedule.setOn(true);
        schedule.setCron(cron);
        return schedule;
    }
}
