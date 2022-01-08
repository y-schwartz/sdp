package org.yschwartz.sdp.schedule.model;

import java.util.concurrent.ScheduledFuture;

import lombok.Data;

@Data
public class ScheduledTask {
    private final BaseSchedule schedule;
    private final ScheduledFuture<?> task;
}
