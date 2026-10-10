package com.akshay.ats.application;

import com.akshay.ats.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stage_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", length = 20)
    private Stage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false, length = 20)
    private Stage toStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(length = 500)
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void prePersist() {
        changedAt = LocalDateTime.now();
    }
}
