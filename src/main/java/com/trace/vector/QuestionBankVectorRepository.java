package com.trace.vector;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class QuestionBankVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuestionBankVectorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据行业和技能，通过语义相似度检索面试题
     */
    public List<Map<String, Object>> findSimilarQuestions(String industry, String skill,
                                                           String queryVector, int limit) {
        String sql = """
                SELECT id, industry, skill, question, difficulty,
                       embedding <=> ?::vector AS distance
                FROM interview_question_bank
                WHERE industry = ? AND skill = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, queryVector, industry, skill, queryVector, limit);
    }

    /**
     * 插入面试题库
     */
    public void insertQuestion(String industry, String skill, String question,
                                String embeddingStr, String difficulty) {
        String sql = """
                INSERT INTO interview_question_bank (industry, skill, question, embedding, difficulty)
                VALUES (?, ?, ?, ?::vector, ?)
                """;
        jdbcTemplate.update(sql, industry, skill, question, embeddingStr, difficulty);
    }

    /**
     * 按行业和技能随机获取题目
     */
    public List<Map<String, Object>> getRandomQuestions(String industry, String skill, int limit) {
        String sql = """
                SELECT id, industry, skill, question, difficulty
                FROM interview_question_bank
                WHERE industry = ? AND skill = ?
                ORDER BY RANDOM()
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, industry, skill, limit);
    }
}
