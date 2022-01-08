package org.yschwartz.sdp.codefunction.controller;

import org.springframework.web.bind.annotation.*;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.codefunction.service.CodeFunctionService;
import org.yschwartz.sdp.rundetails.model.RunDetails;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/function")
public class CodeFunctionController {
    private final CodeFunctionService codeFunctionService;

    public CodeFunctionController(CodeFunctionService codeFunctionService) {
        this.codeFunctionService = codeFunctionService;
    }

    @GetMapping("/{id}")
    public Mono<CodeFunction> get(@PathVariable String id) {
        return codeFunctionService.getCodeFunction(id);
    }

    @GetMapping
    public Flux<CodeFunction> getAll() {
        return codeFunctionService.getAllCodeFunctions();
    }

    @PutMapping
    public Mono<CodeFunction> create(@RequestBody CodeFunction codeFunction) {
        return codeFunctionService.createCodeFunction(codeFunction);
    }

    @PatchMapping
    public Mono<CodeFunction> update(@RequestBody CodeFunction codeFunction) {
        return codeFunctionService.updateCodeFunction(codeFunction);
    }

    @PostMapping("/{id}/trigger")
    public Mono<RunDetails> trigger(@PathVariable String id) {
        return codeFunctionService.triggerCodeFunction(id);
    }

    @PostMapping("/{id}/deploy")
    public Mono<CodeFunction> deploy(@PathVariable String id) {
        return codeFunctionService.deployCodeFunction(id);
    }

    @DeleteMapping("/{id}")
    public Mono<String> delete(@PathVariable String id) {
        return codeFunctionService.deleteCodeFunction(id);
    }

    @DeleteMapping
    public Flux<String> deleteAll() {
        return codeFunctionService.deleteAll();
    }
}
