package org.yschwartz.sdp.common.service;

import static org.yschwartz.sdp.common.config.Constants.ROOT_DIR;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class FileService {

    @Value("${path.base:/etc}")
    private String basePath;

    public enum FileType {
        DOCKERFILE, VOLUME, RESOURCES, LOGS;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public File getFile(FileType fileType, String functionName, String fileName) {
        return getFile(fileType, false, functionName, fileName);
    }

    public File getDirectory(FileType fileType, String functionName) {
        return getFile(fileType, true, functionName);
    }

    public void deleteAll(String functionName) {
        Arrays.stream(FileType.values()).forEach(type -> deleteDirFiles(type, functionName, true));
    }

    public void deleteDirFiles(FileType fileType, String functionName, boolean removeDir) {
        listFiles(fileType, functionName).forEach(File::delete);
        if (removeDir)
            getDirectory(fileType, functionName).delete();
    }

    public List<File> listFiles(FileType fileType, String functionName) {
        return list(getDirectory(fileType, functionName));
    }

    public List<File> listDirectories(FileType fileType) {
        return list(getFile(fileType, true));
    }

    public List<File> list(File file) {
        return Optional.of(file)
                .map(File::listFiles)
                .stream()
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    private File getFile(FileType fileType, boolean isDir, String... path) {
        var file = Path.of(basePath, Stream.concat(Stream.of(ROOT_DIR, fileType.toString()), Arrays.stream(path)).toArray(String[]::new)).toFile();
        var dir = isDir ? file : file.getParentFile();
        if (dir.mkdirs() && log.isDebugEnabled())
            log.debug("Created {} directory for code function: {}", fileType, path[0]);
        return file;
    }
}
