package com.bozidar.tms.user_service.auth;

import com.bozidar.tms.user_service.auth.dto.LoginRequest;
import com.bozidar.tms.user_service.common.exception.InvalidCredentialsException;
import com.bozidar.tms.user_service.user.User;
import com.bozidar.tms.user_service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String login(LoginRequest request) {
        User user = userRepository.findByEmailWithRoles(request.email())
                                  .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.generateToken(user);
    }
}
