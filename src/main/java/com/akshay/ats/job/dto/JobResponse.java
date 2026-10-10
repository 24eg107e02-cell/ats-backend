package com.akshay.ats.job.dto;

import com.akshay.ats.job.EmploymentType;
import com.akshay.ats.job.Job;
import com.akshay.ats.job.JobStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobResponse(
        Long id,
        String title,
        String description,
        String department,
        String location,
        EmploymentType employmentType,
        int minExperience,
        Integer maxExperience,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        String skills,
        JobStatus status,
        int openings,
        String postedByName,
        LocalDate closingDate,
        LocalDateTime createdAt
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(), job.getTitle(), job.getDescription(), job.getDepartment(),
                job.getLocation(), job.getEmploymentType(), job.getMinExperience(),
                job.getMaxExperience(), job.getMinSalary(), job.getMaxSalary(), job.getSkills(),
                job.getStatus(), job.getOpenings(),
                job.getPostedBy() != null ? job.getPostedBy().getFullName() : null,
                job.getClosingDate(), job.getCreatedAt()
        );
    }
}
