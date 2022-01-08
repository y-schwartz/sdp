package org.yschwartz.sdp.schedule.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.yschwartz.sdp.schedule.model.BaseSchedule;
import org.yschwartz.sdp.schedule.model.ScheduledTask;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ScheduleService {

    private final TaskScheduler scheduler;

    private final Map<String, ScheduledTask> tasks = new HashMap<>();

    public ScheduleService(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void createOrUpdateTask(String id, BaseSchedule newSchedule, Runnable task) {
        var currentSchedule = Optional.ofNullable(tasks.get(id)).map(ScheduledTask::getSchedule).orElse(null);
        if (Objects.equals(currentSchedule, newSchedule))
            return;
        if (newSchedule == null || !newSchedule.isOn())
            deleteTask(id);
        else
            createOrUpdate(id, newSchedule, task);
    }

    public void deleteTask(String id) {
        stopTask(id);
        tasks.remove(id);
    }

    public void deleteAll() {
        tasks.forEach((id, x) -> stopTask(id));
        tasks.clear();
    }

    private void createOrUpdate(String id, BaseSchedule schedule, Runnable task) {
        stopTask(id);
        var scheduledTask = scheduler.schedule(task, schedule.getTrigger());
        tasks.put(id, new ScheduledTask(schedule, scheduledTask));
        log.info("Created task {} with schedule: {}", id, schedule);
    }

    private void stopTask(String id) {
        Optional.ofNullable(tasks.get(id)).map(ScheduledTask::getTask).ifPresent(task -> {
            task.cancel(false);
            log.info("Stopped task {}", id);
        });
    }
}
