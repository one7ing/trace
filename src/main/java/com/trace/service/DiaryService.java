package com.trace.service;

import com.trace.dto.DiaryRequest;
import com.trace.entity.Diary;
import com.trace.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final MemoryService memoryService;

    @Transactional
    public Diary create(Long userId, DiaryRequest request) {
        Diary diary = Diary.builder()
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .moodTag(request.getMoodTag())
                .build();

        diary = diaryRepository.save(diary);

        // 双写：向量化存入长期记忆
        try {
            String memoryContent = "【日记】" + request.getTitle() + " | 心情：" + request.getMoodTag() + " | " + request.getContent();
            memoryService.saveLongTermMemory(userId, memoryContent, "diary", diary.getId());
        } catch (Exception e) {
            log.error("Failed to save diary vector memory", e);
        }

        log.info("Diary created: userId={}, diaryId={}", userId, diary.getId());
        return diary;
    }

    public Page<Diary> list(Long userId, Pageable pageable) {
        return diaryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Diary getById(Long id) {
        return diaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("日记不存在"));
    }

    @Transactional
    public Diary update(Long id, DiaryRequest request) {
        Diary diary = getById(id);
        diary.setTitle(request.getTitle());
        diary.setContent(request.getContent());
        diary.setMoodTag(request.getMoodTag());
        return diaryRepository.save(diary);
    }

    @Transactional
    public void delete(Long id) {
        Diary diary = getById(id);
        diaryRepository.delete(diary);
        log.info("Diary deleted: diaryId={}", id);
    }
}
