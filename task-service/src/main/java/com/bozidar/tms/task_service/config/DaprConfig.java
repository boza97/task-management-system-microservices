package com.bozidar.tms.task_service.config;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dapr")
public class DaprConfig {

    @Bean(destroyMethod = "close")
    public DaprClient daprClient() {
        return new DaprClientBuilder()
                .withObjectSerializer(new DaprJacksonSerializer())
                .build();
    }
}
