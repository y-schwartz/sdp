package org.yschwartz.sdp.rundetails.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.yschwartz.sdp.rundetails.model.RunDetails;
import org.yschwartz.sdp.rundetails.service.RunDetailsService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/details")
public class RunDetailsController {
    private final RunDetailsService runDetailsService;

    public RunDetailsController(RunDetailsService runDetailsService) {
        this.runDetailsService = runDetailsService;
    }

    @GetMapping("{id}")
    public Mono<RunDetails> get(@PathVariable String id) {
        return runDetailsService.getRunDetails(id);
    }

    @GetMapping
    public Flux<RunDetails> getAll(@RequestParam(required = false) List<String> functionNames,
                                   @RequestParam(required = false) List<RunDetails.Status> statuses,
                                   @RequestParam(required = false) List<Integer> exitCodes,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return runDetailsService.getAllRunDetails(functionNames, statuses, exitCodes, from, to);
    }
}
