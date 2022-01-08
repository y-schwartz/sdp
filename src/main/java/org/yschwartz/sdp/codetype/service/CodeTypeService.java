package org.yschwartz.sdp.codetype.service;

import java.util.*;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.codetype.exception.CodeTypeNotFoundException;
import org.yschwartz.sdp.codetype.model.CodeType;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

@Service
@Log4j2
@ConfigurationProperties
public class CodeTypeService {
    @Setter
    private List<CodeType> codeTypes = new LinkedList<>();
    private final Map<String, CodeType> codeTypesMap = new HashMap<>();

    @PostConstruct
    public void populateMap() {
        codeTypesMap.clear();
        codeTypes.stream()
                .filter(this::validateType)
                .forEach(type -> codeTypesMap.put(type.getName(), type));
    }

    public CodeType getCodeType(String name) {
        return Optional.ofNullable(codeTypesMap.get(name)).orElseThrow(() -> new CodeTypeNotFoundException(name));
    }

    public Flux<CodeType> getAll() {
        return Flux.fromIterable(codeTypesMap.values());
    }

    private boolean validateType(CodeType codeType) {
        if (Objects.nonNull(codeType) && allNonNull(codeType.getName(), codeType.getFrom(), codeType.getInstallCommand(), codeType.getEntrypoint(), codeType.getMainFileName())) {
            return true;
        }
        log.warn("Loaded code type is missing a mandatory field and will be ignored. code type: {}", codeType);
        return false;
    }

    private static boolean allNonNull(Object... objects) {
        return Arrays.stream(objects).allMatch(Objects::nonNull);
    }
}

