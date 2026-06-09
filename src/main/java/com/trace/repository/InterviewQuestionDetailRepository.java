package com.trace.repository;

import com.trace.entity.InterviewQuestionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionDetailRepository extends JpaRepository<InterviewQuestionDetail, Long> {

    List<InterviewQuestionDetail> findByRecordIdOrderBySequenceNumAsc(Long recordId);
}
