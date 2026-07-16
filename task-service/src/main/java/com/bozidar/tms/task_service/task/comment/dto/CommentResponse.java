package com.bozidar.tms.task_service.task.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String content,
        UUID authorId,
        String authorFullName,
        Instant createdAt
) {
}
