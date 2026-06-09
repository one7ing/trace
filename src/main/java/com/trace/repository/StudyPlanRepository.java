package com.trace.repository;

import com.trace.entity.StudyPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    Page<StudyPlan> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
