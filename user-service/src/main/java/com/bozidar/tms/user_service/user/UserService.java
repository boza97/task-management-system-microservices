package com.bozidar.tms.user_service.user;

import com.bozidar.tms.user_service.common.exception.EmailAlreadyExistsException;
import com.bozidar.tms.user_service.common.exception.ResourceNotFoundException;
import com.bozidar.tms.user_service.user.dto.UserRegistrationRequest;
import com.bozidar.tms.user_service.user.dto.UserResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                             .map(this::toResponse).toList();
    }

    public UserResponse getUser(UUID userId) {
        return toResponse(findUser(userId));
    }

    public List<UserResponse> getUsersByIds(List<UUID> ids) {
        return userRepository.findAllById(ids).stream()
                             .map(this::toResponse).toList();
    }

    public void register(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.firstName(), request.lastName(), request.email(), hashedPassword);

        Role userRole = roleRepository.findByName("USER")
                                      .orElseThrow(() -> new IllegalStateException("Default role USER not found"));
        user.getRoles().add(userRole);

        userRepository.save(user);
    }

    public void assignRole(UUID userId, String roleName) {
        User user = findUser(userId);
        Role role = findRole(roleName);

        user.getRoles().add(role);
    }

    public void removeRole(UUID userId, String roleName) {
        User user = findUser(userId);
        Role role = findRole(roleName);

        user.getRoles().remove(role);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                             .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Role findRole(String roleName) {
        return roleRepository.findByName(roleName)
                             .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
