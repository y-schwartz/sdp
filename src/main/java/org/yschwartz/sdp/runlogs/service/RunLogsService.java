package org.yschwartz.sdp.runlogs.service;

import static org.yschwartz.sdp.common.config.Constants.CONTAINER_NAME_DELIMITER;
import static org.yschwartz.sdp.common.service.FileService.FileType.LOGS;
import static org.yschwartz.sdp.common.util.StringUtils.*;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.common.service.FileService;
import org.yschwartz.sdp.common.util.StringUtils;
import org.yschwartz.sdp.runlogs.exception.ReadLogsException;
import org.yschwartz.sdp.runlogs.exception.RunLogsNotFoundException;

import reactor.core.publisher.Flux;

@Service
public class RunLogsService {

    private final FileService fileService;

    @Value("${logs.header.separator.char:=}")
    private char separatorChar;
    @Value("${logs.header.separator.length:50}")
    private int separatorLength;

    public RunLogsService(FileService fileService) {
        this.fileService = fileService;
    }

    public Flux<String> getLogs(String containerName) {
        var functionName = containerName.split(CONTAINER_NAME_DELIMITER)[0];
        var logFile = fileService.getFile(LOGS, functionName, getLogFileName(containerName));
        return readFile(logFile);
    }

    public Flux<String> getAllLogs(List<String> functionNames, List<String> containerNames, LocalDateTime from, LocalDateTime to) {
        var functionDirs = Optional.ofNullable(functionNames)
                .filter(Predicate.not(List::isEmpty))
                .map(List::stream)
                .map(stream -> stream.map(name -> fileService.getDirectory(LOGS, name)))
                .orElseGet(() -> fileService.listDirectories(LOGS).stream());
        return Flux.fromStream(functionDirs)
                .flatMapIterable(fileService::list)
                .filter(file -> filterByContainerName(file, containerNames))
                .filter(file -> filterByDate(file, from, i -> i >= 0))
                .filter(file -> filterByDate(file, to, i -> i < 0))
                .sort(Comparator.comparing(File::getName).reversed())
                .flatMapSequential(this::readFile);
    }

    public void deleteByDateBefore(LocalDateTime before) {
        fileService.listDirectories(LOGS)
                .stream()
                .map(fileService::list)
                .flatMap(List::stream)
                .filter(file -> filterByDate(file, before, i -> i < 0))
                .forEach(File::delete);
    }

    private Flux<String> readFile(File file) {
        var fileName = file.getName();
        if (!file.exists())
            return Flux.error(() -> new RunLogsNotFoundException(fileName));
        return Flux.concat(getFirstLines(fileName), Flux.using(() -> Files.lines(file.toPath()).map(StringUtils::appendNewLine), Flux::fromStream, Stream::close))
                .onErrorMap(e -> new ReadLogsException(file.getName(), e));
    }

    private Flux<String> getFirstLines(String fileName) {
        return Flux.just(getSeparator(separatorLength, separatorChar), fileName, getSeparator(separatorLength, separatorChar)).map(StringUtils::appendNewLine);
    }

    private static boolean filterByContainerName(File file, List<String> containerNames) {
        return containerNames == null || containerNames.size() == 0
                || containerNames.stream().anyMatch(name -> file.getName().equals(getLogFileName(name)));
    }

    private static boolean filterByDate(File file, LocalDateTime date, Predicate<Integer> compareResultFilter) {
        if (date == null)
            return true;
        var fileTimestamp = getTimestampFromLogFileName(file.getName());
        var dateTimestamp = StringUtils.getFormattedTimestamp(date);
        return compareResultFilter.test(fileTimestamp.compareTo(dateTimestamp));
    }
}
