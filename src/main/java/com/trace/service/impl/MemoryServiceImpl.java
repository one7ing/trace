package com.trace.service.impl;

import com.trace.entity.LongTermMemory;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.service.MemoryService;
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
public class MemoryServiceImpl implements MemoryService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final LongTermMemoryMapper memoryMapper;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private static final int MAX = 10;
    private static final String KEY = "chat:context:";

    public MemoryServiceImpl(RedisTemplate<String, Object> redisTemplate,
                             LongTermMemoryMapper memoryMapper,
                             JdbcTemplate jdbcTemplate,
                             @Autowired(required = false) EmbeddingModel embeddingModel) {
        this.redisTemplate = redisTemplate; this.memoryMapper = memoryMapper;
        this.jdbcTemplate = jdbcTemplate; this.embeddingModel = embeddingModel;
    }

    @Override public void saveChatContext(Long userId, String role, String content) {
        String k = KEY + userId; redisTemplate.opsForList().rightPush(k, Map.of("role", role, "content", content));
        Long s = redisTemplate.opsForList().size(k); if (s != null && s > MAX * 2) redisTemplate.opsForList().trim(k, -MAX * 2, -1);
    }
    @Override @SuppressWarnings("unchecked")
    public List<Map<String, String>> getChatContext(Long userId) {
        List<Object> e = redisTemplate.opsForList().range(KEY + userId, 0, -1);
        if (e == null) return List.of(); return e.stream().map(o -> (Map<String, String>) o).toList();
    }
    @Override public void saveLongTermMemory(Long userId, String content, String sourceType, Long sourceId) {
        if (embeddingModel == null) return;
        float[] emb = embeddingModel.embed(content);
        StringBuilder sb = new StringBuilder("["); for (int i = 0; i < emb.length; i++) { if (i > 0) sb.append(","); sb.append(emb[i]); } sb.append("]");
        jdbcTemplate.update("INSERT INTO long_term_memories (user_id,content,embedding,source_type,source_id,created_at) VALUES (?,?,?::vector,?,?,CURRENT_TIMESTAMP)", userId, content, sb.toString(), sourceType, sourceId);
    }
    @Override public List<LongTermMemory> retrieveMemories(Long userId, String query, int limit) {
        if (embeddingModel == null) return List.of();
        float[] emb = embeddingModel.embed(query);
        StringBuilder sb = new StringBuilder("["); for (int i = 0; i < emb.length; i++) { if (i > 0) sb.append(","); sb.append(emb[i]); } sb.append("]");
        return jdbcTemplate.query("SELECT id,user_id,content,source_type,source_id,created_at FROM long_term_memories WHERE user_id=? ORDER BY embedding <=> ?::vector LIMIT ?",
                (rs, n) -> LongTermMemory.builder().id(rs.getLong("id")).userId(rs.getLong("user_id")).content(rs.getString("content")).sourceType(rs.getString("source_type")).sourceId(rs.getLong("source_id")).createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : java.time.LocalDateTime.now()).build(), userId, sb.toString(), limit);
    }
    @Override public List<LongTermMemory> getRecentMemories(Long userId, List<String> sourceTypes, int limit) { return memoryMapper.findRecentByUserId(userId); }
}
