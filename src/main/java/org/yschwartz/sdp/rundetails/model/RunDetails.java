package org.yschwartz.sdp.rundetails.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
public class RunDetails {
    @Id
    private String id;
    private String functionName;
    private Status status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer exitCode;
    private String errorMessage;

    public enum Status {IN_PROGRESS, SUCCESS, FAILURE}
}
