package org.yschwartz.sdp.codetype.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yschwartz.sdp.codetype.model.CodeType;
import org.yschwartz.sdp.codetype.service.CodeTypeService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/type")
public class CodeTypeController {
    private final CodeTypeService codeTypeService;

    public CodeTypeController(CodeTypeService codeTypeService) {
        this.codeTypeService = codeTypeService;
    }

    @GetMapping
    public Flux<CodeType> getAll() {
        return codeTypeService.getAll();
    }
}
