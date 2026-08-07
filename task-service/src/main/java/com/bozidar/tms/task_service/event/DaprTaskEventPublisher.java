package com.bozidar.tms.task_service.event;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("dapr")
public class DaprTaskEventPublisher implements TaskEventPublisher {

    private final DaprClient daprClient;
    private final String pubsubName;
    private final String topic;

    public DaprTaskEventPublisher(DaprClient daprClient,
                                  @Value("${dapr.pubsub.name}") String pubsubName,
                                  @Value("${app.kafka.topics.task-events}") String topic) {
        this.daprClient = daprClient;
        this.pubsubName = pubsubName;
        this.topic = topic;
    }

    @Override
    public void publish(TaskEvent event) {
        daprClient.publishEvent(
                pubsubName,
                topic,
                event,
                Map.of("partitionKey", event.taskId().toString())
        ).block();
    }
}
