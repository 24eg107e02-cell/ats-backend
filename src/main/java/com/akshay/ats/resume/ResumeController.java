package com.akshay.ats.resume;

import com.akshay.ats.common.AccessDeniedCustomException;
import com.akshay.ats.common.ResourceNotFoundException;
import com.akshay.ats.user.Role;
import com.akshay.ats.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    public record ResumeResponse(Long id, String fileName, long fileSize, String contentType) {}

    private final ResumeRepository resumeRepository;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                     @AuthenticationPrincipal User candidate) throws IOException {
        String storedPath = fileStorageService.store(file);

        Resume resume = Resume.builder()
                .candidate(candidate)
                .fileName(file.getOriginalFilename())
                .storedPath(storedPath)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();

        resume = resumeRepository.save(resume);
        return ResponseEntity.ok(new ResumeResponse(resume.getId(), resume.getFileName(), resume.getFileSize(), resume.getContentType()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                              @AuthenticationPrincipal User requester) throws MalformedURLException {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + id));

        boolean isOwner = resume.getCandidate().getId().equals(requester.getId());
        boolean isRecruiterOrAdmin = requester.getRole() == Role.RECRUITER || requester.getRole() == Role.ADMIN;
        if (!isOwner && !isRecruiterOrAdmin) {
            throw new AccessDeniedCustomException("You do not have permission to download this resume");
        }

        Resource fileResource = new UrlResource(fileStorageService.resolve(resume.getStoredPath()).toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resume.getFileName() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(resume.getContentType()))
                .body(fileResource);
    }
}
