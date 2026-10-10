package com.akshay.ats.auth;

import com.akshay.ats.auth.dto.AuthResponse;
import com.akshay.ats.auth.dto.LoginRequest;
import com.akshay.ats.auth.dto.RegisterRequest;
import com.akshay.ats.config.JwtService;
import com.akshay.ats.user.Role;
import com.akshay.ats.user.User;
import com.akshay.ats.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be created through registration");
        }
        // Candidates self-register. Admin/Recruiter accounts should be created by an existing admin
        // in a real deployment — left open here to keep the training project simple to demo.
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .role(request.role() == null ? Role.CANDIDATE : request.role())
                .enabled(true)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
