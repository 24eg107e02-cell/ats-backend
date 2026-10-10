package com.akshay.ats.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FeedbackRequest(
        @NotBlank String feedback,
        @Min(1) @Max(5) Integer rating
) {}
