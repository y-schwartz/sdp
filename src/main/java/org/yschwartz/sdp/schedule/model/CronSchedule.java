package org.yschwartz.sdp.schedule.model;

import java.util.TimeZone;

import org.springframework.scheduling.support.CronTrigger;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CronSchedule extends BaseSchedule {
    private String cron;

    @Override
    public CronTrigger getTrigger() {
        return new CronTrigger(cron, TimeZone.getTimeZone(TimeZone.getDefault().getID()));
    }
}
