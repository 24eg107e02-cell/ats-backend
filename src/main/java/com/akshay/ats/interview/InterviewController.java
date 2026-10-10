package com.akshay.ats.interview;

import com.akshay.ats.interview.dto.FeedbackRequest;
import com.akshay.ats.interview.dto.InterviewRequest;
import com.akshay.ats.interview.dto.InterviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/api/applications/{applicationId}/interviews")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<InterviewResponse> schedule(@PathVariable Long applicationId,
                                                        @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(interviewService.schedule(applicationId, request));
    }

    @PatchMapping("/api/interviews/{id}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<InterviewResponse> reschedule(@PathVariable Long id,
                                                          @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(interviewService.reschedule(id, request));
    }

    @PatchMapping("/api/interviews/{id}/feedback")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<InterviewResponse> feedback(@PathVariable Long id,
                                                        @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(interviewService.submitFeedback(id, request));
    }

    @DeleteMapping("/api/interviews/{id}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        interviewService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
