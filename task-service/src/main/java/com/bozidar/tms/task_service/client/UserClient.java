package com.bozidar.tms.task_service.client;

import com.bozidar.tms.task_service.client.dto.UserResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface UserClient {

    Optional<UserResponse> getUser(UUID userId);

    List<UserResponse> getUsersByIds(Collection<UUID> userIds);

    default Map<UUID, UserResponse> getUsersMappedByIds(Collection<UUID> userIds) {
        return getUsersByIds(userIds).stream()
                                     .collect(Collectors.toMap(UserResponse::id, Function.identity()));
    }
}
