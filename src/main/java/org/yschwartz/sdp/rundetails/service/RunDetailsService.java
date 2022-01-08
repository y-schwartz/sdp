package org.yschwartz.sdp.rundetails.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.rundetails.model.RunDetails;
import org.yschwartz.sdp.rundetails.repository.RunDetailsRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RunDetailsService {
    private final RunDetailsRepository runDetailsRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public RunDetailsService(RunDetailsRepository runDetailsRepository, ReactiveMongoTemplate reactiveMongoTemplate) {
        this.runDetailsRepository = runDetailsRepository;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    public Mono<RunDetails> saveInProgressDetails(String id, String functionName) {
        var runDetails = new RunDetails();
        runDetails.setId(id);
        runDetails.setFunctionName(functionName);
        runDetails.setStartTime(LocalDateTime.now());
        runDetails.setStatus(RunDetails.Status.IN_PROGRESS);
        return runDetailsRepository.save(runDetails);
    }

    public Mono<RunDetails> saveSuccessDetails(RunDetails runDetails, int exitCode) {
        runDetails.setExitCode(exitCode);
        runDetails.setStatus(RunDetails.Status.SUCCESS);
        runDetails.setEndTime(LocalDateTime.now());
        return runDetailsRepository.save(runDetails);
    }

    public Mono<RunDetails> saveFailureDetails(RunDetails runDetails, String errorMessage) {
        runDetails.setErrorMessage(errorMessage);
        runDetails.setStatus(RunDetails.Status.FAILURE);
        runDetails.setEndTime(LocalDateTime.now());
        return runDetailsRepository.save(runDetails);
    }

    public Mono<Void> deleteAllRunDetails(String functionName) {
        return runDetailsRepository.deleteAllByFunctionName(functionName);
    }

    public Mono<RunDetails> getRunDetails(String id) {
        return runDetailsRepository.findById(id);
    }

    public Flux<RunDetails> getAllRunDetails(List<String> functionNames, List<RunDetails.Status> statuses, List<Integer> exitCodes, LocalDateTime from, LocalDateTime to) {
        var query = new Query();
        addCriteria(query, "functionName", functionNames);
        addCriteria(query, "status", statuses);
        addCriteria(query, "exitCode", exitCodes);
        addCriteria(query, from, to);
        return reactiveMongoTemplate.find(query, RunDetails.class);
    }

    private static <T> void addCriteria(Query query, String field, List<T> in) {
        Optional.ofNullable(in)
                .filter(Predicate.not(List::isEmpty))
                .ifPresent(list -> query.addCriteria(Criteria.where(field).in(list)));
    }

    private static void addCriteria(Query query, LocalDateTime from, LocalDateTime to) {
        var criteria = Criteria.where("startTime");
        var fromOpt = Optional.ofNullable(from);
        var toOpt = Optional.ofNullable(to);
        fromOpt.ifPresent(criteria::gte);
        toOpt.ifPresent(criteria::lt);
        fromOpt.or(() -> toOpt).ifPresent(x -> query.addCriteria(criteria));
    }
}
