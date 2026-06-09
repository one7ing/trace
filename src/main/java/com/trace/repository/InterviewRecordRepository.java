package com.trace.repository;

import com.trace.entity.InterviewRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRecordRepository extends JpaRepository<InterviewRecord, Long> {

    Page<InterviewRecord> findByUserIdOrderByCompletedAtDesc(Long userId, Pageable pageable);

    List<InterviewRecord> findByUserIdAndCompletedAtBetweenOrderByCompletedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
}
