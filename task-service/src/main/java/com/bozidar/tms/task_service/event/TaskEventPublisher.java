package com.bozidar.tms.task_service.event;

public interface TaskEventPublisher {

    void publish(TaskEvent event);
}
