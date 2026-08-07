package com.bozidar.tms.project_service.client;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("dapr")
public class DaprTaskClient implements TaskClient {

    private final DaprClient daprClient;
    private final String appId;

    public DaprTaskClient(DaprClient daprClient,
                          @Value("${services.task-service.app-id}") String appId) {
        this.daprClient = daprClient;
        this.appId = appId;
    }

    @Override
    public boolean hasAssignedTasks(UUID projectId, UUID userId) {
        Map<String, List<String>> queryParams = Map.of(
                "projectId", List.of(projectId.toString()),
                "assigneeId", List.of(userId.toString())
        );
        HttpExtension httpExtension = new HttpExtension(HttpExtension.GET.getMethod(), queryParams, authHeaders());

        Boolean result = daprClient.invokeMethod(appId, "api/tasks/assignments/exists", null,
                                                 httpExtension, Boolean.class).block();
        return Boolean.TRUE.equals(result);
    }

    private Map<String, String> authHeaders() {
        Map<String, String> headers = new HashMap<>();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                headers.put(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return headers;
    }
}
