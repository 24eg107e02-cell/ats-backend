package com.akshay.ats.job;

import org.springframework.data.jpa.domain.Specification;

public class JobSpecifications {

    public static Specification<Job> hasStatus(JobStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Job> titleOrDescriptionContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
            );
        };
    }

    public static Specification<Job> locationContains(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isBlank()) return null;
            return cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
        };
    }

    public static Specification<Job> hasEmploymentType(EmploymentType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("employmentType"), type);
    }

    public static Specification<Job> minExperienceAtMost(Integer maxExp) {
        return (root, query, cb) -> maxExp == null ? null : cb.lessThanOrEqualTo(root.get("minExperience"), maxExp);
    }
}
