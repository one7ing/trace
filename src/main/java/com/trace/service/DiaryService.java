package com.trace.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.DiaryRequest;
import com.trace.entity.Diary;

public interface DiaryService {
    Diary create(Long userId, DiaryRequest request);
    IPage<Diary> list(Long userId, int page, int size);
    Diary getById(Long id);
    Diary update(Long id, DiaryRequest request);
    void delete(Long id);
}
