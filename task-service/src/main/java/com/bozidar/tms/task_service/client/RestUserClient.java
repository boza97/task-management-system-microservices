package com.bozidar.tms.task_service.client;

import com.bozidar.tms.task_service.client.dto.UserResponse;
import com.bozidar.tms.task_service.config.ResilienceExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Profile("!dapr")
public class RestUserClient implements UserClient {

    private static final String CB_NAME = "user-service";

    private final RestClient restClient;
    private final ResilienceExecutor resilience;

    public RestUserClient(RestClient.Builder builder,
                          ResilienceExecutor resilience,
                          @Value("${services.user-service.url}") String userServiceUrl) {
        this.resilience = resilience;
        this.restClient = builder
                .baseUrl(userServiceUrl)
                .requestInterceptor((request, body, execution) -> {
                    forwardAuthorizationHeader(request.getHeaders());
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public Optional<UserResponse> getUser(UUID userId) {
        try {
            return Optional.ofNullable(
                    resilience.execute(CB_NAME, () ->
                            restClient.get()
                                      .uri("/api/users/{id}", userId)
                                      .retrieve()
                                      .body(UserResponse.class)));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public List<UserResponse> getUsersByIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        String ids = userIds.stream()
                            .map(UUID::toString)
                            .collect(Collectors.joining(","));

        List<UserResponse> users = resilience.execute(CB_NAME, () ->
                restClient.get()
                          .uri(uriBuilder -> uriBuilder.path("/api/users")
                                                       .queryParam("ids", ids)
                                                       .build())
                          .retrieve()
                          .body(new ParameterizedTypeReference<>() {
                          }));

        return users != null ? users : List.of();
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
