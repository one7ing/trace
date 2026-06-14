package com.trace.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trace.dto.DiaryRequest;
import com.trace.entity.Diary;
import com.trace.mapper.DiaryMapper;
import com.trace.service.DiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryMapper diaryMapper;

    @Override @Transactional
    public Diary create(Long userId, DiaryRequest request) {
        Diary diary = Diary.builder().userId(userId).title(request.getTitle())
                .content(request.getContent()).moodTag(request.getMoodTag()).build();
        diaryMapper.insert(diary);
        return diary;
    }

    @Override
    public IPage<Diary> list(Long userId, int page, int size) {
        Page<Diary> mpPage = new Page<>(page + 1, size);
        List<Diary> all = diaryMapper.findByUserIdOrderByCreatedAtDesc(userId);
        // simple pagination
        int start = (int) mpPage.offset();
        int end = Math.min(start + (int) mpPage.getSize(), all.size());
        mpPage.setRecords(all.subList(Math.min(start, all.size()), end));
        mpPage.setTotal(all.size());
        return mpPage;
    }

    @Override
    public Diary getById(Long id) {
        Diary d = diaryMapper.selectById(id);
        if (d == null) throw new IllegalArgumentException("日记不存在");
        return d;
    }

    @Override @Transactional
    public Diary update(Long id, DiaryRequest request) {
        Diary d = getById(id);
        d.setTitle(request.getTitle()); d.setContent(request.getContent()); d.setMoodTag(request.getMoodTag());
        diaryMapper.updateById(d);
        return d;
    }

    @Override @Transactional
    public void delete(Long id) { getById(id); diaryMapper.deleteById(id); }
}
