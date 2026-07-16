package com.bozidar.tms.task_service.client;

import com.bozidar.tms.task_service.client.dto.ProjectMemberResponse;
import com.bozidar.tms.task_service.client.dto.ProjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Component
public class ProjectClient {

    private final RestClient restClient;

    public ProjectClient(RestClient.Builder builder,
                         @Value("${services.project-service.url}") String projectServiceUrl) {
        this.restClient = builder
                .baseUrl(projectServiceUrl)
                .requestInterceptor((request, body, execution) -> {
                    forwardAuthorizationHeader(request.getHeaders());
                    return execution.execute(request, body);
                })
                .build();
    }

    public Optional<ProjectResponse> getProject(UUID projectId) {
        try {
            return Optional.ofNullable(
                    restClient.get()
                              .uri("/api/projects/{id}", projectId)
                              .retrieve()
                              .body(ProjectResponse.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public Optional<ProjectMemberResponse> getMembership(UUID projectId, UUID userId) {
        try {
            return Optional.ofNullable(
                    restClient.get()
                              .uri("/api/projects/{projectId}/members/{userId}", projectId, userId)
                              .retrieve()
                              .body(ProjectMemberResponse.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public boolean isMember(UUID projectId, UUID userId) {
        return getMembership(projectId, userId).isPresent();
    }

    public boolean hasRole(UUID projectId, UUID userId, String role) {
        return getMembership(projectId, userId)
                .map(m -> role.equals(m.role()))
                .orElse(false);
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
