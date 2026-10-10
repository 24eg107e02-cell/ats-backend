package com.akshay.ats.auth.dto;

import com.akshay.ats.user.Role;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String fullName,
        Role role
) {}
