package com.bozidar.tms.task_service.client;

import com.bozidar.tms.task_service.client.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient.Builder builder,
                      @Value("${services.user-service.url}") String userServiceUrl) {
        this.restClient = builder
                .baseUrl(userServiceUrl)
                .requestInterceptor((request, body, execution) -> {
                    forwardAuthorizationHeader(request.getHeaders());
                    return execution.execute(request, body);
                })
                .build();
    }

    public Optional<UserResponse> getUser(UUID userId) {
        try {
            return Optional.ofNullable(
                    restClient.get()
                              .uri("/api/users/{id}", userId)
                              .retrieve()
                              .body(UserResponse.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public List<UserResponse> getUsersByIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        String ids = userIds.stream()
                            .map(UUID::toString)
                            .collect(Collectors.joining(","));

        List<UserResponse> users = restClient.get()
                                             .uri(uriBuilder -> uriBuilder.path("/api/users")
                                                                          .queryParam("ids", ids)
                                                                          .build())
                                             .retrieve()
                                             .body(new ParameterizedTypeReference<>() {
                                             });

        return users != null ? users : List.of();
    }

    public Map<UUID, UserResponse> getUsersMappedByIds(Collection<UUID> userIds) {
        return getUsersByIds(userIds).stream()
                                     .collect(Collectors.toMap(UserResponse::id, Function.identity()));
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
