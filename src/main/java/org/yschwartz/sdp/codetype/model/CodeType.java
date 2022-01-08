package org.yschwartz.sdp.codetype.model;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;

@Data
public class CodeType {
    private String name;
    private String from;
    private List<String> preInstallCommands = new LinkedList<>();
    private String installCommand;
    private List<String> postInstallCommands = new LinkedList<>();
    private String entrypoint;
    private String mainFileName;
}