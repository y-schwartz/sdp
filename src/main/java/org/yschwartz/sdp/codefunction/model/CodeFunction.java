package org.yschwartz.sdp.codefunction.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.yschwartz.sdp.schedule.model.BaseSchedule;

import lombok.Data;

@Data
@Document
public class CodeFunction {
    @Id
    private String name;
    private List<KeyValuePair> tags;
    // build
    private String codeTypeName;
    private List<String> dependencies;
    private List<String> additionalCommands;
    // run
    private List<KeyValuePair> environmentVariables;
    private List<Volume> volumes;
    private NetworkMode networkMode;
    private Boolean privileged;
    private Integer runTimeout;
    // deploy
    private DeploymentDetails deploymentDetails;
    private BaseSchedule schedule;

    private LocalDateTime creationDate;
    private LocalDateTime lastUpdated;

    public enum NetworkMode {HOST, NONE}

    @Data
    public static class Volume {
        private String hostPath;
        private String containerPath;
        private boolean readOnly;
    }

    @Data
    public static class KeyValuePair {
        private String key;
        private String value;
    }
}
