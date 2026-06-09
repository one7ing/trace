package com.trace.service;

import com.trace.entity.LongTermMemory;
import com.trace.vector.LongTermMemoryVectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MemoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final LongTermMemoryVectorRepository memoryRepository;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public MemoryService(RedisTemplate<String, Object> redisTemplate,
                         LongTermMemoryVectorRepository memoryRepository,
                         JdbcTemplate jdbcTemplate,
                         @Autowired(required = false) EmbeddingModel embeddingModel) {
        this.redisTemplate = redisTemplate;
        this.memoryRepository = memoryRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
    }

    private static final int SHORT_TERM_MAX_ROUNDS = 10;
    private static final String CHAT_CONTEXT_KEY = "chat:context:";

    // ============ 短期记忆 (Redis) ============

    public void saveChatContext(Long userId, String role, String content) {
        String key = CHAT_CONTEXT_KEY + userId;
        Map<String, String> entry = Map.of("role", role, "content", content);
        redisTemplate.opsForList().rightPush(key, entry);

        // 只保留最近 10 轮
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > SHORT_TERM_MAX_ROUNDS * 2) {
            redisTemplate.opsForList().trim(key, -SHORT_TERM_MAX_ROUNDS * 2, -1);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getChatContext(Long userId) {
        String key = CHAT_CONTEXT_KEY + userId;
        List<Object> entries = redisTemplate.opsForList().range(key, 0, -1);
        if (entries == null) return List.of();
        return entries.stream()
                .map(e -> (Map<String, String>) e)
                .toList();
    }

    // ============ 长期记忆 (PgVector) ============

    public void saveLongTermMemory(Long userId, String content, String sourceType, Long sourceId) {
        if (embeddingModel == null) {
            log.debug("EmbeddingModel not available, skipping vector storage");
            return;
        }
        float[] embedding = embeddingModel.embed(content);
        String embeddingStr = vectorToString(embedding);

        jdbcTemplate.update(
                "INSERT INTO long_term_memories (user_id, content, embedding, source_type, source_id) VALUES (?, ?, ?::vector, ?, ?)",
                userId, content, embeddingStr, sourceType, sourceId
        );

        log.debug("Saved long-term memory: userId={}, sourceType={}, sourceId={}", userId, sourceType, sourceId);
    }

    public List<LongTermMemory> retrieveMemories(Long userId, String query, int limit) {
        if (embeddingModel == null) {
            log.debug("EmbeddingModel not available, returning empty memories");
            return List.of();
        }
        float[] queryEmbedding = embeddingModel.embed(query);
        String queryVector = vectorToString(queryEmbedding);

        String sql = """
                SELECT id, user_id, content, source_type, source_id, created_at
                FROM long_term_memories
                WHERE user_id = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> LongTermMemory.builder()
                        .id(rs.getLong("id"))
                        .userId(rs.getLong("user_id"))
                        .content(rs.getString("content"))
                        .sourceType(rs.getString("source_type"))
                        .sourceId(rs.getLong("source_id"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build(),
                userId, queryVector, limit);
    }

    public List<LongTermMemory> getRecentMemories(Long userId, List<String> sourceTypes, int limit) {
        return memoryRepository.findRecentByUserIdAndSourceTypes(userId, sourceTypes, limit);
    }

    // ============ Utils ============

    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
