package com.bozidar.tms.task_service.client.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName
) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
