package com.bozidar.tms.task_service.task.comment;

import com.bozidar.tms.task_service.client.UserClient;
import com.bozidar.tms.task_service.client.dto.UserResponse;
import com.bozidar.tms.task_service.common.exception.ResourceNotFoundException;
import com.bozidar.tms.task_service.security.CurrentUser;
import com.bozidar.tms.task_service.security.CurrentUserProvider;
import com.bozidar.tms.task_service.task.Task;
import com.bozidar.tms.task_service.task.TaskRepository;
import com.bozidar.tms.task_service.task.comment.dto.CommentCreateRequest;
import com.bozidar.tms.task_service.task.comment.dto.CommentResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserClient userClient;

    @Override
    public CommentResponse addComment(UUID taskId, CommentCreateRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Comment content must not be empty");
        }

        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        Task task = taskRepository.findById(taskId)
                                  .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthorId(currentUser.id());
        comment.setContent(request.content().trim());

        comment = commentRepository.save(comment);

        // TODO(events): objaviti COMMENT_ADDED dogadjaj (audit-service)

        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                currentUser.id(),
                currentUser.fullName(),
                comment.getCreatedAt()
        );
    }

    @Override
    public List<CommentResponse> getComments(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found");
        }

        List<Comment> comments = commentRepository.findAllByTaskIdOrderByCreatedAtDesc(taskId);

        Set<UUID> authorIds = comments.stream()
                                      .map(Comment::getAuthorId)
                                      .collect(Collectors.toSet());

        Map<UUID, UserResponse> authors = userClient.getUsersMappedByIds(authorIds);

        return comments.stream()
                       .map(comment -> {
                           UserResponse author = authors.get(comment.getAuthorId());
                           return new CommentResponse(
                                   comment.getId(),
                                   comment.getContent(),
                                   comment.getAuthorId(),
                                   author != null ? author.fullName() : null,
                                   comment.getCreatedAt()
                           );
                       })
                       .toList();
    }

    @Override
    public void deleteComment(UUID taskId, UUID commentId) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found");
        }

        Comment comment = commentRepository.findById(commentId)
                                           .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getTask().getId().equals(taskId)) {
            throw new IllegalArgumentException("Comment does not belong to this task");
        }

        boolean isAuthor = comment.getAuthorId().equals(currentUser.id());
        boolean isAdmin = currentUser.hasRole("ADMIN");

        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException("Only comment author or admin can delete comment");
        }

        // TODO(events): objaviti COMMENT_DELETED dogadjaj (audit-service)

        commentRepository.delete(comment);
    }
}
