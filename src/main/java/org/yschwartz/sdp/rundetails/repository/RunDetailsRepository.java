package org.yschwartz.sdp.rundetails.repository;

import java.time.LocalDateTime;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.yschwartz.sdp.rundetails.model.RunDetails;

import reactor.core.publisher.Mono;

@Repository
public interface RunDetailsRepository extends ReactiveCrudRepository<RunDetails, String> {
    Mono<Void> deleteAllByFunctionName(String functionName);

    Mono<Void> deleteAllByEndTimeBefore(LocalDateTime before);
}
