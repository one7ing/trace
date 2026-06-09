package com.trace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "interview_question_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "sequence_num")
    private int sequenceNum;
}
