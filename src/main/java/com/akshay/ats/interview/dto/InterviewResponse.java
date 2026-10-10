package com.akshay.ats.interview.dto;

import com.akshay.ats.interview.Interview;
import com.akshay.ats.interview.InterviewMode;
import com.akshay.ats.interview.InterviewStatus;

import java.time.LocalDateTime;

public record InterviewResponse(
        Long id,
        Long applicationId,
        Long interviewerId,
        String interviewerName,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        InterviewMode mode,
        String locationOrLink,
        InterviewStatus status,
        String feedback,
        Integer rating
) {
    public static InterviewResponse from(Interview i) {
        return new InterviewResponse(
                i.getId(), i.getApplication().getId(), i.getInterviewer().getId(),
                i.getInterviewer().getFullName(), i.getScheduledStart(), i.getScheduledEnd(),
                i.getMode(), i.getLocationOrLink(), i.getStatus(), i.getFeedback(), i.getRating()
        );
    }
}
