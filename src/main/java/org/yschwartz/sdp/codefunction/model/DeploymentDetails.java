package org.yschwartz.sdp.codefunction.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Transient;
import org.yschwartz.sdp.docker.service.DockerBuildService;

import lombok.Data;

@Data
public class DeploymentDetails {
    private LocalDateTime lastDeployed;
    @Transient
    private boolean pendingChanges;
    @Transient
    private DockerBuildService.Status buildStatus;
}
