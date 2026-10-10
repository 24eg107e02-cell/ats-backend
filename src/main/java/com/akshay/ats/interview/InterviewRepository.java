package com.akshay.ats.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query("""
            SELECT i FROM Interview i
            WHERE i.interviewer.id = :interviewerId
              AND i.status = 'SCHEDULED'
              AND i.scheduledStart < :end
              AND i.scheduledEnd > :start
            """)
    List<Interview> findOverlapping(@Param("interviewerId") Long interviewerId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    List<Interview> findByInterviewerIdAndScheduledStartBetween(Long interviewerId, LocalDateTime from, LocalDateTime to);
}
