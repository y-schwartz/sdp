package org.yschwartz.sdp.docker.service;

import static org.yschwartz.sdp.common.config.Constants.CONTAINER_ROOT;
import static org.yschwartz.sdp.common.config.Constants.NEW_LINE;
import static org.yschwartz.sdp.common.service.FileService.FileType.DOCKERFILE;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.codetype.model.CodeType;
import org.yschwartz.sdp.codetype.service.CodeTypeService;
import org.yschwartz.sdp.common.service.FileService;
import org.yschwartz.sdp.docker.exception.DockerfileCreationException;
import org.yschwartz.try_utils.functional.ThrowingConsumer;
import org.yschwartz.try_utils.model.Try;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class DockerfileService {

    private final CodeTypeService codeTypeService;
    private final FileService fileService;

    public DockerfileService(CodeTypeService codeTypeService, FileService fileService) {
        this.codeTypeService = codeTypeService;
        this.fileService = fileService;
    }

    public File getDockerfile(CodeFunction codeFunction) {
        var codeType = codeTypeService.getCodeType(codeFunction.getCodeTypeName());
        int hash = Objects.hash(codeFunction.getDependencies(), codeFunction.getAdditionalCommands(), codeType);
        for (File file : fileService.listFiles(DOCKERFILE, codeFunction.getName())) {
            if (file.getName().startsWith(String.valueOf(hash)))
                return file;
            if (file.delete() && log.isDebugEnabled())
                log.debug("Deleted old dockerfile: {} for code function: {}", file.getName(), codeFunction.getName());
        }
        return createDockerfile(codeFunction, codeType, hash);
    }

    private File createDockerfile(CodeFunction codeFunction, CodeType codeType, int hash) {
        var dockerfile = fileService.getFile(DOCKERFILE, codeFunction.getName(), "%s_%s".formatted(hash, LocalDateTime.now()));
        Try.of(() -> new FileOutputStream(dockerfile))
                .flatMap(fos -> Try.of(fos, (ThrowingConsumer<FileOutputStream>) f -> writeDockerfile(codeFunction, codeType, f)))
                .catchAny()
                .thenThrow(e -> new DockerfileCreationException(codeFunction.getName(), e))
                .execute();
        log.info("Saved new dockerfile for code function: {} at {}", codeFunction.getName(), dockerfile);
        return dockerfile;
    }

    private void writeDockerfile(CodeFunction codeFunction, CodeType codeType, FileOutputStream outputStream) {
        appendFromLine(outputStream, codeType.getFrom());
        codeType.getPreInstallCommands().forEach(command -> appendRunLine(outputStream, command));
        Optional.of(codeFunction.getDependencies())
                .map(list -> String.join(" ", list))
                .filter(Predicate.not(String::isBlank))
                .map(s -> "%s %s".formatted(codeType.getInstallCommand(), s))
                .ifPresent(s -> appendRunLine(outputStream, s));
        codeType.getPostInstallCommands().forEach(command -> appendRunLine(outputStream, command));
        codeFunction.getAdditionalCommands().forEach(command -> appendRunLine(outputStream, command));
        appendEntrypointLine(outputStream, codeType.getEntrypoint());
        appendCmdLine(outputStream, "%s/%s".formatted(CONTAINER_ROOT, codeType.getMainFileName()));
    }

    private static void appendFromLine(FileOutputStream outputStream, String from) {
        appendLine(outputStream, "FROM ", from);
    }

    private static void appendRunLine(FileOutputStream outputStream, String run) {
        appendLine(outputStream, "RUN ", run);
    }

    private static void appendEntrypointLine(FileOutputStream outputStream, String entrypoint) {
        appendLine(outputStream, "ENTRYPOINT [\"", entrypoint, "\"]");
    }

    private static void appendCmdLine(FileOutputStream outputStream, String cmd) {
        appendLine(outputStream, "CMD [\"", cmd, "\"]");
    }

    private static void appendLine(FileOutputStream outputStream, String... parts) {
        var line = String.join("", parts) + NEW_LINE;
        Try.of(() -> outputStream.write(line.getBytes())).execute();
    }
}
