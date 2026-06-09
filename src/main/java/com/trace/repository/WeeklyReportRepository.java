package com.trace.repository;

import com.trace.entity.WeeklyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    Page<WeeklyReport> findByUserIdOrderByWeekStartDesc(Long userId, Pageable pageable);

    Optional<WeeklyReport> findByUserIdAndWeekStart(Long userId, java.time.LocalDate weekStart);
}
