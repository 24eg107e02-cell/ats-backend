package com.akshay.ats.interview.dto;

import com.akshay.ats.interview.InterviewMode;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record InterviewRequest(
        @NotNull Long interviewerId,
        @NotNull @Future LocalDateTime scheduledStart,
        @NotNull @Future LocalDateTime scheduledEnd,
        @NotNull InterviewMode mode,
        String locationOrLink
) {}
