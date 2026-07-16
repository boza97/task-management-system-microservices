package com.bozidar.tms.task_service.task.dto;

import com.bozidar.tms.task_service.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskUpdateRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 5000)
        String description,

        @NotNull
        TaskPriority priority,
        LocalDate dueDate
) {
}