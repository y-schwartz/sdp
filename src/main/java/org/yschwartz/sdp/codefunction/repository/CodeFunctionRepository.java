package org.yschwartz.sdp.codefunction.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import org.yschwartz.sdp.codefunction.model.CodeFunction;

import reactor.core.publisher.Flux;

@Repository
public interface CodeFunctionRepository extends ReactiveMongoRepository<CodeFunction, String> {
    Flux<CodeFunction> findAllByCodeTypeName(String typeName);
}
