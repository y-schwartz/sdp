package org.yschwartz.sdp.codefunction.service;

import static org.yschwartz.sdp.common.service.FileService.FileType.RESOURCES;
import static org.yschwartz.sdp.common.service.FileService.FileType.VOLUME;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.codefunction.exception.CodeFunctionAlreadyDeployedException;
import org.yschwartz.sdp.codefunction.exception.CodeFunctionDeploymentException;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.codetype.service.CodeTypeService;
import org.yschwartz.sdp.common.service.FileService;
import org.yschwartz.try_utils.model.Try;

@Service
public class FunctionDeploymentService {

    private final CodeTypeService codeTypeService;
    private final FileService fileService;

    @Value("${deploy.retry.delay:200}")
    private int delay;

    public FunctionDeploymentService(CodeTypeService codeTypeService, FileService fileService) {
        this.codeTypeService = codeTypeService;
        this.fileService = fileService;
    }

    public void createMainFile(CodeFunction codeFunction) {
        var functionName = codeFunction.getName();
        var mainFileName = codeTypeService.getCodeType(codeFunction.getCodeTypeName()).getMainFileName();
        Try.of(() -> fileService.getFile(RESOURCES, functionName, mainFileName).createNewFile())
                .catchAny()
                .thenThrow(e -> new CodeFunctionDeploymentException(functionName, e))
                .retry()
                .fixedDelay(delay)
                .execute();
    }

    public void deploy(String functionName) {
        if (!hasChanges(functionName))
            throw new CodeFunctionAlreadyDeployedException(functionName);
        var resourcesDir = fileService.getDirectory(RESOURCES, functionName);
        var volumeDir = fileService.getDirectory(VOLUME, functionName);
        Try.of(() -> fileService.deleteDirFiles(VOLUME, functionName, false))
                .andThen(() -> FileUtils.copyDirectory(resourcesDir, volumeDir))
                .catchAny()
                .thenThrow(e -> new CodeFunctionDeploymentException(functionName, e))
                .retry()
                .fixedDelay(delay)
                .execute();
    }

    public boolean hasChanges(String functionName) {
        var resourcesFiles = fileService.listFiles(RESOURCES, functionName);
        if (resourcesFiles.size() != fileService.listFiles(VOLUME, functionName).size())
            return true;
        return resourcesFiles.stream().anyMatch(file -> didFileChange(file, functionName));
    }

    private boolean didFileChange(File resourceFile, String functionName) {
        var volumeFile = fileService.getFile(VOLUME, functionName, resourceFile.getName());
        return !volumeFile.exists() || FileUtils.isFileNewer(resourceFile, volumeFile);
    }
}
