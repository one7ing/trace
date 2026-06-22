package com.trace.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.PracticeStartRequest;
import com.trace.dto.PracticeAnswerRequest;
import com.trace.entity.PracticeRecord;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 刷题服务 —— 题库抽题 + 提交答案 + 异步判题。
 */
public interface PracticeService {

    Map<String, Object> startPractice(Long userId, PracticeStartRequest request);

    Map<String, Object> submitPractice(String sessionId, PracticeAnswerRequest request);

    Flux<String> startPracticeStream(Long userId, PracticeStartRequest request);

    Flux<String> submitPracticeStream(String sessionId, PracticeAnswerRequest request);

    IPage<PracticeRecord> getRecords(Long userId, int page, int size);

    PracticeRecord getRecordDetail(Long recordId);

    void abortPractice(String sessionId);

    void deleteRecord(Long id);
}
