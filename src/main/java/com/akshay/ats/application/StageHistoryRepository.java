package com.akshay.ats.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageHistoryRepository extends JpaRepository<StageHistory, Long> {
    List<StageHistory> findByApplicationIdOrderByChangedAtAsc(Long applicationId);
}
