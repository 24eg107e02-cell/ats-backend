package com.akshay.ats.application.dto;

import com.akshay.ats.application.Application;
import com.akshay.ats.application.ApplicationStatus;
import com.akshay.ats.application.Stage;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        Long candidateId,
        String candidateName,
        Long resumeId,
        String coverLetter,
        Stage currentStage,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime updatedAt
) {
    public static ApplicationResponse from(Application app) {
        return new ApplicationResponse(
                app.getId(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getCandidate().getId(),
                app.getCandidate().getFullName(),
                app.getResume() != null ? app.getResume().getId() : null,
                app.getCoverLetter(),
                app.getCurrentStage(),
                app.getStatus(),
                app.getAppliedAt(),
                app.getUpdatedAt()
        );
    }
}
