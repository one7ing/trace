package com.trace.vector;

import com.trace.entity.LongTermMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LongTermMemoryVectorRepository extends JpaRepository<LongTermMemory, Long> {

    @Query(value = """
            SELECT * FROM long_term_memories
            WHERE user_id = :userId
              AND source_type IN (:sourceTypes)
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<LongTermMemory> findRecentByUserIdAndSourceTypes(
            @Param("userId") Long userId,
            @Param("sourceTypes") List<String> sourceTypes,
            @Param("limit") int limit);

    List<LongTermMemory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LongTermMemory> findByUserIdAndSourceTypeOrderByCreatedAtDesc(Long userId, String sourceType);
}
