package com.akshay.ats.job;

import com.akshay.ats.common.PageResponse;
import com.akshay.ats.job.dto.JobRequest;
import com.akshay.ats.job.dto.JobResponse;
import com.akshay.ats.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<PageResponse<JobResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EmploymentType type,
            @RequestParam(required = false) Integer maxExperience,
            @RequestParam(required = false, defaultValue = "OPEN") JobStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal User requester) {
        if (!isStaff(requester)) {
            status = JobStatus.OPEN;   // anonymous users and candidates only ever see open jobs
        }

        return ResponseEntity.ok(PageResponse.from(
                jobService.search(q, location, type, maxExperience, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getById(@PathVariable Long id,
                                               @AuthenticationPrincipal User requester) {
        JobResponse job = jobService.getById(id);
        if (job.status() == JobStatus.DRAFT && !isStaff(requester)) {
            throw new com.akshay.ats.common.ResourceNotFoundException("Job not found with id: " + id);
        }
        return ResponseEntity.ok(job);
    }

    private static boolean isStaff(User u) {
        return u != null && (u.getRole() == com.akshay.ats.user.Role.RECRUITER
                || u.getRole() == com.akshay.ats.user.Role.ADMIN);
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobRequest request,
                                               @AuthenticationPrincipal User recruiter) {
        return ResponseEntity.ok(jobService.create(request, recruiter));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<JobResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody JobRequest request,
                                               @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(jobService.update(id, request, requester));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<JobResponse> updateStatus(@PathVariable Long id,
                                                      @RequestParam JobStatus status,
                                                      @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(jobService.updateStatus(id, status, requester));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
