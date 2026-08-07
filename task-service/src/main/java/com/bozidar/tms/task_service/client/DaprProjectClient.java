package com.bozidar.tms.task_service.client;

import com.bozidar.tms.task_service.client.dto.ProjectMemberResponse;
import com.bozidar.tms.task_service.client.dto.ProjectResponse;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.exceptions.DaprException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("dapr")
public class DaprProjectClient implements ProjectClient {

    private final DaprClient daprClient;
    private final String appId;

    public DaprProjectClient(DaprClient daprClient,
                             @Value("${services.project-service.app-id}") String appId) {
        this.daprClient = daprClient;
        this.appId = appId;
    }

    @Override
    public Optional<ProjectResponse> getProject(UUID projectId) {
        return invoke("api/projects/" + projectId, ProjectResponse.class);
    }

    @Override
    public Optional<ProjectMemberResponse> getMembership(UUID projectId, UUID userId) {
        return invoke("api/projects/" + projectId + "/members/" + userId, ProjectMemberResponse.class);
    }

    private <T> Optional<T> invoke(String method, Class<T> type) {
        HttpExtension httpExtension = new HttpExtension(HttpExtension.GET.getMethod(), Map.of(), authHeaders());
        try {
            return Optional.ofNullable(
                    daprClient.invokeMethod(appId, method, null, httpExtension, type).block());
        } catch (DaprException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return Optional.empty();
            }
            throw e;
        }
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
