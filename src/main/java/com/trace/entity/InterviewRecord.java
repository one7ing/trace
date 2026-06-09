package com.trace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "interview_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "questionDetails")
public class InterviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String industry;

    @Column(name = "skill_tags", columnDefinition = "TEXT[]")
    private List<String> skillTags;

    @Column(name = "total_questions")
    private int totalQuestions;

    @Column(name = "avg_score", precision = 5, scale = 2)
    private BigDecimal avgScore;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @CreationTimestamp
    @Column(name = "completed_at", updatable = false)
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "recordId", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNum ASC")
    private List<InterviewQuestionDetail> questionDetails;
}
