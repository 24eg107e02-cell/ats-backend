package com.akshay.ats.application.dto;

import com.akshay.ats.application.Stage;
import jakarta.validation.constraints.NotNull;

public record StageChangeRequest(
        @NotNull Stage toStage,
        String note
) {}
