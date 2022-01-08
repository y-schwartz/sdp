package org.yschwartz.sdp.runlogs.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.yschwartz.sdp.runlogs.service.RunLogsService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/logs")
public class RunLogsController {
    private final RunLogsService runLogsService;

    public RunLogsController(RunLogsService runLogsService) {
        this.runLogsService = runLogsService;
    }

    @GetMapping(value = "{id}/log.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> get(@PathVariable String id) {
        return runLogsService.getLogs(id);
    }

    @GetMapping(value = "{functionName}/logs.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> getAll(@PathVariable String functionName,
                               @RequestParam(required = false) List<String> containerNames,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return runLogsService.getAllLogs(List.of(functionName), containerNames, from, to);
    }
}
