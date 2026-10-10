package com.akshay.ats.interview;

import com.akshay.ats.application.Application;
import com.akshay.ats.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(name = "scheduled_start", nullable = false)
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private LocalDateTime scheduledEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewMode mode;

    @Column(name = "location_or_link", length = 500)
    private String locationOrLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private Integer rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = InterviewStatus.SCHEDULED;
    }
}
