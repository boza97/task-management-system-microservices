package com.bozidar.tms.notification_service.security;

import java.util.List;
import java.util.UUID;

public record CurrentUser(
        UUID id,
        String email,
        String firstName,
        String lastName,
        List<String> roles
) {

    public boolean hasRole(String roleName) {
        return roles != null && roles.contains(roleName);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
