package org.yschwartz.sdp.schedule.model;

import org.springframework.scheduling.Trigger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CronSchedule.class, name = "CRON"),
        @JsonSubTypes.Type(value = FixedDelaySchedule.class, name = "FIXED_DELAY")
})
public abstract class BaseSchedule {
    private boolean on;

    public abstract Trigger getTrigger();
}
