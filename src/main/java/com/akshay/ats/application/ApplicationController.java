package com.akshay.ats.application;

import com.akshay.ats.application.dto.*;
import com.akshay.ats.common.PageResponse;
import com.akshay.ats.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/api/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApplicationResponse> apply(@PathVariable Long jobId,
                                                       @RequestBody ApplyRequest request,
                                                       @AuthenticationPrincipal User candidate) {
        return ResponseEntity.ok(applicationService.apply(jobId, request, candidate));
    }

    @GetMapping("/api/applications/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<PageResponse<ApplicationResponse>> myApplications(
            @AuthenticationPrincipal User candidate,
            @PageableDefault(size = 20, sort = "appliedAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(applicationService.getMyApplications(candidate.getId(), pageable)));
    }

    @GetMapping("/api/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ApplicationResponse>> forJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) Stage stage,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedAt") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(applicationService.getForJob(jobId, stage, status, pageable)));
    }

    @GetMapping("/api/applications/{id}")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable Long id,
                                                         @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(applicationService.getById(id, requester));
    }

    @GetMapping("/api/applications/{id}/history")
    public ResponseEntity<List<StageHistoryResponse>> history(@PathVariable Long id,
                                                                @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(applicationService.getHistory(id, requester));
    }

    @PatchMapping("/api/applications/{id}/stage")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<ApplicationResponse> changeStage(@PathVariable Long id,
                                                            @Valid @RequestBody StageChangeRequest request,
                                                            @AuthenticationPrincipal User recruiter) {
        return ResponseEntity.ok(applicationService.changeStage(id, request.toStage(), request.note(), recruiter));
    }

    @PatchMapping("/api/applications/{id}/reject")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<ApplicationResponse> reject(@PathVariable Long id,
                                                        @Valid @RequestBody RejectRequest request,
                                                        @AuthenticationPrincipal User recruiter) {
        return ResponseEntity.ok(applicationService.reject(id, request.reason(), recruiter));
    }

    @PatchMapping("/api/applications/{id}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApplicationResponse> withdraw(@PathVariable Long id,
                                                          @AuthenticationPrincipal User candidate) {
        return ResponseEntity.ok(applicationService.withdraw(id, candidate));
    }

    @PatchMapping("/api/applications/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationResponse> reopen(@PathVariable Long id,
                                                        @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(applicationService.reopen(id, admin));
    }
}
