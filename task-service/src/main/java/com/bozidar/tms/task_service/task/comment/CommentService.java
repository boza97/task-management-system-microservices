package com.bozidar.tms.task_service.task.comment;

import com.bozidar.tms.task_service.task.comment.dto.CommentCreateRequest;
import com.bozidar.tms.task_service.task.comment.dto.CommentResponse;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentResponse addComment(UUID taskId, CommentCreateRequest request);

    List<CommentResponse> getComments(UUID taskId);

    void deleteComment(UUID taskId, UUID commentId);
}
