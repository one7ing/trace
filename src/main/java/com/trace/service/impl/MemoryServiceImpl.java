package com.trace.service.impl;

import com.trace.entity.LongTermMemory;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 双层记忆服务实现 —— Redis 短期记忆 + PgVector 长期记忆。
 * 所有数据库操作通过 MyBatis-Plus Mapper / XML 完成。
 */
@Slf4j
@Service
public class MemoryServiceImpl implements MemoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final LongTermMemoryMapper memoryMapper;
    private final EmbeddingModel embeddingModel;

    private static final int MAX_ROUNDS = 10;
    private static final String KEY_PREFIX = "chat:context:";

    public MemoryServiceImpl(RedisTemplate<String, Object> redisTemplate,
                              LongTermMemoryMapper memoryMapper,
                              @Autowired(required = false) EmbeddingModel embeddingModel) {
        this.redisTemplate = redisTemplate;
        this.memoryMapper = memoryMapper;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void saveChatContext(Long userId, String role, String content) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForList().rightPush(key, Map.of("role", role, "content", content));
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_ROUNDS * 2) {
            redisTemplate.opsForList().trim(key, -MAX_ROUNDS * 2, -1);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getChatContext(Long userId) {
        try {
            List<Object> entries = redisTemplate.opsForList().range(
                    KEY_PREFIX + userId, 0, -1);
            if (entries == null) {
                return List.of();
            }
            return entries.stream()
                    .map(e -> (Map<String, String>) e)
                    .toList();
        } catch (Exception e) {
            log.debug("Failed to deserialize chat context, clearing: {}", e.getMessage());
            redisTemplate.delete(KEY_PREFIX + userId);
            return List.of();
        }
    }

    @Override
    public void saveLongTermMemory(Long userId,
                                   String content,
                                   String sourceType,
                                   Long sourceId) {
        if (embeddingModel == null) {
            return;
        }
        float[] embedding = embeddingModel.embed(content);
        String vecStr = vectorToString(embedding);
        memoryMapper.insertVector(userId, content, vecStr, sourceType, sourceId);
    }

    @Override
    public List<LongTermMemory> retrieveMemories(Long userId, String query, int limit) {
        if (embeddingModel == null) {
            return List.of();
        }
        float[] emb = embeddingModel.embed(query);
        String vecStr = vectorToString(emb);
        return memoryMapper.retrieveByVector(userId, vecStr, limit);
    }

    @Override
    public List<LongTermMemory> getRecentMemories(Long userId,
                                                  List<String> sourceTypes,
                                                  int limit) {
        return memoryMapper.findRecentByUserId(userId);
    }

    /**
     * 将 float[] 转为 PostgreSQL vector 格式字符串。
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        return sb.append("]").toString();
    }
}
