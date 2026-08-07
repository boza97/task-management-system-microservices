package com.bozidar.tms.task_service.client;

import com.bozidar.tms.task_service.client.dto.ProjectMemberResponse;
import com.bozidar.tms.task_service.client.dto.ProjectResponse;
import com.bozidar.tms.task_service.config.ResilienceExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Component
@Profile("!dapr")
public class RestProjectClient implements ProjectClient {

    private static final String CB_NAME = "project-service";

    private final RestClient restClient;
    private final ResilienceExecutor resilience;

    public RestProjectClient(RestClient.Builder builder,
                             ResilienceExecutor resilience,
                             @Value("${services.project-service.url}") String projectServiceUrl) {
        this.resilience = resilience;
        this.restClient = builder
                .baseUrl(projectServiceUrl)
                .requestInterceptor((request, body, execution) -> {
                    forwardAuthorizationHeader(request.getHeaders());
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public Optional<ProjectResponse> getProject(UUID projectId) {
        try {
            return Optional.ofNullable(
                    resilience.execute(CB_NAME, () ->
                            restClient.get()
                                      .uri("/api/projects/{id}", projectId)
                                      .retrieve()
                                      .body(ProjectResponse.class)));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ProjectMemberResponse> getMembership(UUID projectId, UUID userId) {
        try {
            return Optional.ofNullable(
                    resilience.execute(CB_NAME, () ->
                            restClient.get()
                                      .uri("/api/projects/{projectId}/members/{userId}", projectId, userId)
                                      .retrieve()
                                      .body(ProjectMemberResponse.class)));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
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
