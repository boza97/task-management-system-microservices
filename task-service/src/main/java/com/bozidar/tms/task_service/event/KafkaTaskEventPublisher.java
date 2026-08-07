package com.bozidar.tms.task_service.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!dapr")
public class KafkaTaskEventPublisher implements TaskEventPublisher {

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;
    private final String topic;

    public KafkaTaskEventPublisher(KafkaTemplate<String, TaskEvent> kafkaTemplate,
                                   @Value("${app.kafka.topics.task-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(TaskEvent event) {
        kafkaTemplate.send(topic, event.taskId().toString(), event);
    }
}
