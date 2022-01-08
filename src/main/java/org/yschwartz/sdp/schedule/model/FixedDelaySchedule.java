package org.yschwartz.sdp.schedule.model;

import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.support.PeriodicTrigger;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FixedDelaySchedule extends BaseSchedule {
    private long delay;
    private TimeUnit timeUnit;

    @Override
    public PeriodicTrigger getTrigger() {
        return new PeriodicTrigger(delay, timeUnit);
    }
}

