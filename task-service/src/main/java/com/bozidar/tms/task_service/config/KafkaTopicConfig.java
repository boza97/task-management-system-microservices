package com.bozidar.tms.task_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("!dapr")
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.task-events}")
    private String taskEventsTopic;

    @Bean
    public NewTopic taskEventsTopicDefinition() {
        return TopicBuilder.name(taskEventsTopic)
                           .partitions(1)
                           .replicas(1)
                           .build();
    }
}
