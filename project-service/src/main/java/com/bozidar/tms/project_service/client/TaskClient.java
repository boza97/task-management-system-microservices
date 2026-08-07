package com.bozidar.tms.project_service.client;

import java.util.UUID;

public interface TaskClient {

    boolean hasAssignedTasks(UUID projectId, UUID userId);
}
