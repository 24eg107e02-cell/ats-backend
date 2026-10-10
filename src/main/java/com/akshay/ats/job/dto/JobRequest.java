package com.akshay.ats.job.dto;

import com.akshay.ats.job.EmploymentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobRequest(
        @NotBlank String title,
        @NotBlank String description,
        String department,
        String location,
        @NotNull EmploymentType employmentType,
        @Min(0) int minExperience,
        Integer maxExperience,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        String skills,
        @Min(1) int openings,
        LocalDate closingDate
) {}
