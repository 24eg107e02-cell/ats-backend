package com.akshay.ats.application;

import com.akshay.ats.application.dto.ApplicationResponse;
import com.akshay.ats.application.dto.ApplyRequest;
import com.akshay.ats.application.dto.StageHistoryResponse;
import com.akshay.ats.common.AccessDeniedCustomException;
import com.akshay.ats.common.DuplicateApplicationException;
import com.akshay.ats.common.ResourceNotFoundException;
import com.akshay.ats.job.Job;
import com.akshay.ats.job.JobRepository;
import com.akshay.ats.resume.Resume;
import com.akshay.ats.resume.ResumeRepository;
import com.akshay.ats.user.Role;
import com.akshay.ats.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final StageTransitionValidator transitionValidator;

    @Transactional
    public ApplicationResponse apply(Long jobId, ApplyRequest request, User candidate) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // Belt-and-suspenders: check first for a friendly error message, but the real
        // guarantee is the unique constraint on (job_id, candidate_id) at the DB level,
        // caught below - that's what actually prevents a race condition under concurrent requests.
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidate.getId())) {
            throw new DuplicateApplicationException("You have already applied to this job");
        }

        Resume resume = null;
        if (request.resumeId() != null) {
            resume = resumeRepository.findById(request.resumeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + request.resumeId()));
            if (!resume.getCandidate().getId().equals(candidate.getId())) {
                throw new AccessDeniedCustomException("This resume does not belong to you");
            }
        }

        Application application = Application.builder()
                .job(job)
                .candidate(candidate)
                .resume(resume)
                .coverLetter(request.coverLetter())
                .currentStage(Stage.APPLIED)
                .status(ApplicationStatus.ACTIVE)
                .build();

        try {
            application = applicationRepository.save(application);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateApplicationException("You have already applied to this job");
        }

        recordHistory(application, null, Stage.APPLIED, candidate, "Application submitted");

        return ApplicationResponse.from(application);
    }

    public Page<ApplicationResponse> getMyApplications(Long candidateId, Pageable pageable) {
        return applicationRepository.findByCandidateId(candidateId, pageable).map(ApplicationResponse::from);
    }

    public Page<ApplicationResponse> getForJob(Long jobId, Stage stage, ApplicationStatus status, Pageable pageable) {
        if (stage != null) {
            return applicationRepository.findByJobIdAndCurrentStage(jobId, stage, pageable).map(ApplicationResponse::from);
        }
        if (status != null) {
            return applicationRepository.findByJobIdAndStatus(jobId, status, pageable).map(ApplicationResponse::from);
        }
        return applicationRepository.findByJobId(jobId, pageable).map(ApplicationResponse::from);
    }

    public ApplicationResponse getById(Long id, User requester) {
        Application app = findEntity(id);
        assertCanView(app, requester);
        return ApplicationResponse.from(app);
    }

    public List<StageHistoryResponse> getHistory(Long applicationId, User requester) {
        Application app = findEntity(applicationId);
        assertCanView(app, requester);
        return stageHistoryRepository.findByApplicationIdOrderByChangedAtAsc(applicationId)
                .stream().map(StageHistoryResponse::from).toList();
    }

    @Transactional
    public ApplicationResponse changeStage(Long id, Stage toStage, String note, User recruiter) {
        Application app = findEntity(id);

        if (app.getStatus() != ApplicationStatus.ACTIVE) {
            throw new com.akshay.ats.common.InvalidStageTransitionException(
                    "Cannot change stage - application status is " + app.getStatus());
        }

        Stage from = app.getCurrentStage();
        transitionValidator.validate(from, toStage);

        app.setCurrentStage(toStage);
        if (toStage == Stage.HIRED) {
            app.setStatus(ApplicationStatus.HIRED);
        }

        recordHistory(app, from, toStage, recruiter, note);

        return ApplicationResponse.from(app);
    }

    @Transactional
    public ApplicationResponse reject(Long id, String reason, User recruiter) {
        Application app = findEntity(id);

        if (app.getStatus() != ApplicationStatus.ACTIVE) {
            throw new com.akshay.ats.common.InvalidStageTransitionException(
                    "Cannot reject - application status is already " + app.getStatus());
        }

        Stage from = app.getCurrentStage();
        app.setStatus(ApplicationStatus.REJECTED);

        recordHistory(app, from, from, recruiter, "Rejected: " + reason);

        return ApplicationResponse.from(app);
    }

    @Transactional
    public ApplicationResponse withdraw(Long id, User candidate) {
        Application app = findEntity(id);

        if (!app.getCandidate().getId().equals(candidate.getId())) {
            throw new AccessDeniedCustomException("You can only withdraw your own applications");
        }
        if (app.getStatus() != ApplicationStatus.ACTIVE) {
            throw new com.akshay.ats.common.InvalidStageTransitionException(
                    "Cannot withdraw - application status is already " + app.getStatus());
        }

        Stage from = app.getCurrentStage();
        app.setStatus(ApplicationStatus.WITHDRAWN);

        recordHistory(app, from, from, candidate, "Withdrawn by candidate");

        return ApplicationResponse.from(app);
    }

    @Transactional
    public ApplicationResponse reopen(Long id, User admin) {
        Application app = findEntity(id);
        Stage from = app.getCurrentStage();
        app.setStatus(ApplicationStatus.ACTIVE);
        recordHistory(app, from, from, admin, "Reopened by admin");
        return ApplicationResponse.from(app);
    }

    // ---- helpers ----

    private Application findEntity(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }

    private void assertCanView(Application app, User requester) {
        boolean isCandidate = app.getCandidate().getId().equals(requester.getId());
        boolean isRecruiterOrAdmin = requester.getRole() == Role.RECRUITER || requester.getRole() == Role.ADMIN;
        if (!isCandidate && !isRecruiterOrAdmin) {
            throw new AccessDeniedCustomException("You do not have permission to view this application");
        }
    }

    private void recordHistory(Application app, Stage from, Stage to, User changedBy, String note) {
        StageHistory history = StageHistory.builder()
                .application(app)
                .fromStage(from)
                .toStage(to)
                .changedBy(changedBy)
                .note(note)
                .build();
        stageHistoryRepository.save(history);
    }
}
