package com.bozidar.tms.project_service.client;

import com.bozidar.tms.project_service.client.dto.UserResponse;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.exceptions.DaprException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Profile("dapr")
public class DaprUserClient implements UserClient {

    private final DaprClient daprClient;
    private final String appId;

    public DaprUserClient(DaprClient daprClient,
                          @Value("${services.user-service.app-id}") String appId) {
        this.daprClient = daprClient;
        this.appId = appId;
    }

    @Override
    public Optional<UserResponse> getUser(UUID userId) {
        HttpExtension httpExtension = new HttpExtension(HttpExtension.GET.getMethod(), Map.of(), authHeaders());
        try {
            return Optional.ofNullable(
                    daprClient.invokeMethod(appId, "api/users/" + userId, null,
                                            httpExtension, UserResponse.class).block());
        } catch (DaprException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return Optional.empty();
            }
            throw e;
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

        Map<String, List<String>> queryParams = Map.of("ids", List.of(ids));
        HttpExtension httpExtension = new HttpExtension(HttpExtension.GET.getMethod(), queryParams, authHeaders());

        UserResponse[] users = daprClient.invokeMethod(appId, "api/users", null,
                                                       httpExtension, UserResponse[].class).block();

        return users != null ? List.of(users) : List.of();
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
