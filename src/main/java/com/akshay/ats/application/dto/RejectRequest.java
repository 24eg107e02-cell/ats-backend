package com.akshay.ats.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank String reason
) {}
