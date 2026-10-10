-- =========================================================
-- V1__init_schema.sql
-- Applicant Tracking System - initial schema
-- =========================================================

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(150)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(150)    NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20)     NOT NULL,              -- ADMIN, RECRUITER, CANDIDATE
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE jobs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(150)    NOT NULL,
    description     TEXT            NOT NULL,
    department      VARCHAR(100),
    location        VARCHAR(150),
    employment_type VARCHAR(20)     NOT NULL,              -- FULL_TIME, PART_TIME, CONTRACT, INTERN
    min_experience  INT             NOT NULL DEFAULT 0,
    max_experience  INT,
    min_salary      DECIMAL(12,2),
    max_salary      DECIMAL(12,2),
    skills          VARCHAR(500),
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT', -- DRAFT, OPEN, CLOSED
    openings        INT             NOT NULL DEFAULT 1,
    posted_by       BIGINT          NOT NULL,
    closing_date    DATE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_jobs_posted_by FOREIGN KEY (posted_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE resumes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id    BIGINT          NOT NULL,
    file_name       VARCHAR(255)    NOT NULL,
    stored_path     VARCHAR(500)    NOT NULL,
    file_size       BIGINT          NOT NULL,
    content_type    VARCHAR(100)    NOT NULL,
    uploaded_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resumes_candidate FOREIGN KEY (candidate_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE applications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id          BIGINT          NOT NULL,
    candidate_id    BIGINT          NOT NULL,
    resume_id       BIGINT,
    cover_letter    TEXT,
    current_stage   VARCHAR(20)     NOT NULL DEFAULT 'APPLIED', -- APPLIED, SCREENING, INTERVIEW, OFFER, HIRED
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, REJECTED, WITHDRAWN, HIRED
    applied_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT fk_app_candidate FOREIGN KEY (candidate_id) REFERENCES users(id),
    CONSTRAINT fk_app_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    CONSTRAINT uq_job_candidate UNIQUE (job_id, candidate_id)   -- prevents duplicate applications at the DB level
) ENGINE=InnoDB;

CREATE TABLE stage_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id  BIGINT          NOT NULL,
    from_stage      VARCHAR(20),
    to_stage        VARCHAR(20)     NOT NULL,
    changed_by      BIGINT          NOT NULL,
    note            VARCHAR(500),
    changed_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_app FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE interviews (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id      BIGINT          NOT NULL,
    interviewer_id       BIGINT          NOT NULL,
    scheduled_start     DATETIME        NOT NULL,
    scheduled_end       DATETIME        NOT NULL,
    mode                VARCHAR(20)     NOT NULL,              -- ONSITE, VIDEO, PHONE
    location_or_link    VARCHAR(500),
    status              VARCHAR(20)     NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
    feedback            TEXT,
    rating              INT,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interview_app FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_interview_user FOREIGN KEY (interviewer_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Helpful indexes for the lookups the API does most often
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_applications_job ON applications(job_id);
CREATE INDEX idx_applications_candidate ON applications(candidate_id);
CREATE INDEX idx_applications_stage ON applications(current_stage);
CREATE INDEX idx_interviews_time ON interviews(interviewer_id, scheduled_start, scheduled_end);
