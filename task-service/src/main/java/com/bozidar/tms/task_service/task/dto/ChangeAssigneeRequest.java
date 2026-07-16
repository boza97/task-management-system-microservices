package com.bozidar.tms.task_service.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "ChangeAssigneeRequest", description = "Assign/unassign a task")
public record ChangeAssigneeRequest(
        @Schema(example = "d43376db-aca0-4db7-a1be-25fae98951f9",
                description = "Assignee user ID. If null then unassign.")
        UUID assigneeId
) {
}
