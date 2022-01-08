package org.yschwartz.sdp.docker.mapper;

import static org.yschwartz.sdp.common.config.Constants.CONTAINER_ROOT;
import static org.yschwartz.sdp.common.config.Constants.IMAGE_TAG;
import static org.yschwartz.sdp.common.service.FileService.FileType.VOLUME;
import static org.yschwartz.sdp.common.util.FunctionalUtil.getOrCreate;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yschwartz.sdp.codefunction.model.CodeFunction;
import org.yschwartz.sdp.common.service.FileService;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;

@Component
public class DockerInputMapper {
    private final DockerClient dockerClient;
    private final FileService fileService;

    @Value("${path.volume:}")
    private String volumePath;

    public DockerInputMapper(DockerClient dockerClient, FileService fileService) {
        this.dockerClient = dockerClient;
        this.fileService = fileService;
    }

    public CreateContainerCmd mapCodeFunctionToCreateCommandInput(CodeFunction codeFunction, String containerName) {
        var functionName = codeFunction.getName();
        var command = dockerClient.createContainerCmd(IMAGE_TAG.formatted(functionName)).withName(containerName);
        var hostConfig = getOrCreate(command, CreateContainerCmd::getHostConfig, CreateContainerCmd::withHostConfig, HostConfig::newHostConfig);
        command.withEnv(codeFunction.getEnvironmentVariables().stream().map(entry -> "%s=%s" .formatted(entry.getKey(), entry.getValue())).collect(Collectors.toList()));
        setVolumes(hostConfig, codeFunction.getVolumes(), functionName);
        hostConfig.withNetworkMode(codeFunction.getNetworkMode().toString().toLowerCase()).withPrivileged(codeFunction.getPrivileged());
        return command;
    }

    private void setVolumes(HostConfig hostConfig, List<CodeFunction.Volume> volumes, String functionName) {
        var hostPath = Optional.ofNullable(volumePath)
                .filter(Predicate.not(String::isBlank))
                .map(path -> new File(path, functionName))
                .orElseGet(() -> fileService.getDirectory(VOLUME, functionName))
                .getAbsolutePath();
        var binds = new LinkedList<>(List.of(getBind(hostPath, CONTAINER_ROOT, false)));
        volumes.stream()
                .map(volume -> getBind(volume.getHostPath(), volume.getContainerPath(), volume.isReadOnly()))
                .forEach(binds::add);
        hostConfig.withBinds(binds);
    }

    private static Bind getBind(String hostPath, String containerPath, boolean readOnly) {
        return new Bind(hostPath, new Volume(containerPath), readOnly ? AccessMode.ro : AccessMode.rw);
    }
}
