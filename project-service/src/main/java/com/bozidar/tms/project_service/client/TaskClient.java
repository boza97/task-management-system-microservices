package com.bozidar.tms.project_service.client;

import com.bozidar.tms.project_service.config.ResilienceExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class TaskClient {

    private static final String CB_NAME = "task-service";

    private final RestClient restClient;
    private final ResilienceExecutor resilience;

    public TaskClient(RestClient.Builder builder,
                      ResilienceExecutor resilience,
                      @Value("${services.task-service.url}") String taskServiceUrl) {
        this.resilience = resilience;
        this.restClient = builder
                .baseUrl(taskServiceUrl)
                .requestInterceptor((request, body, execution) -> {
                    forwardAuthorizationHeader(request.getHeaders());
                    return execution.execute(request, body);
                })
                .build();
    }

    public boolean hasAssignedTasks(UUID projectId, UUID userId) {
        Boolean result = resilience.execute(CB_NAME, () ->
                restClient.get()
                          .uri(uriBuilder -> uriBuilder.path("/api/tasks/assignments/exists")
                                                       .queryParam("projectId", projectId)
                                                       .queryParam("assigneeId", userId)
                                                       .build())
                          .retrieve()
                          .body(Boolean.class));

        return Boolean.TRUE.equals(result);
    }

    private void forwardAuthorizationHeader(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
    }
}
