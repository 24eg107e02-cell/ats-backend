package com.akshay.ats.job;

import com.akshay.ats.common.ResourceNotFoundException;
import com.akshay.ats.job.dto.JobRequest;
import com.akshay.ats.job.dto.JobResponse;
import com.akshay.ats.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.akshay.ats.job.JobSpecifications.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobRepository;

    public Page<JobResponse> search(String q, String location, EmploymentType type,
                                     Integer maxExperience, JobStatus status, Pageable pageable) {
        Specification<Job> spec = Specification.where(hasStatus(status))
                .and(titleOrDescriptionContains(q))
                .and(locationContains(location))
                .and(hasEmploymentType(type))
                .and(minExperienceAtMost(maxExperience));

        return jobRepository.findAll(spec, pageable).map(JobResponse::from);
    }

    public JobResponse getById(Long id) {
        return JobResponse.from(findEntity(id));
    }

    @Transactional
    public JobResponse create(JobRequest request, User recruiter) {
        Job job = Job.builder()
                .title(request.title())
                .description(request.description())
                .department(request.department())
                .location(request.location())
                .employmentType(request.employmentType())
                .minExperience(request.minExperience())
                .maxExperience(request.maxExperience())
                .minSalary(request.minSalary())
                .maxSalary(request.maxSalary())
                .skills(request.skills())
                .status(JobStatus.DRAFT)
                .openings(request.openings())
                .postedBy(recruiter)
                .closingDate(request.closingDate())
                .build();

        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public JobResponse update(Long id, JobRequest request, User requester) {
        Job job = findEntity(id);
        assertOwnerOrAdmin(job, requester);

        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setDepartment(request.department());
        job.setLocation(request.location());
        job.setEmploymentType(request.employmentType());
        job.setMinExperience(request.minExperience());
        job.setMaxExperience(request.maxExperience());
        job.setMinSalary(request.minSalary());
        job.setMaxSalary(request.maxSalary());
        job.setSkills(request.skills());
        job.setOpenings(request.openings());
        job.setClosingDate(request.closingDate());

        return JobResponse.from(job); // managed entity - flushed on commit
    }

    @Transactional
    public JobResponse updateStatus(Long id, JobStatus status, User requester) {
        Job job = findEntity(id);
        assertOwnerOrAdmin(job, requester);
        job.setStatus(status);
        return JobResponse.from(job);
    }

    @Transactional
    public void delete(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found with id: " + id);
        }
        jobRepository.deleteById(id);
    }

    private Job findEntity(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    private void assertOwnerOrAdmin(Job job, User requester) {
        boolean isOwner = job.getPostedBy() != null && job.getPostedBy().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole().name().equals("ADMIN");
        if (!isOwner && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to modify this job");
        }
    }
}
