package com.trace.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.InterviewAnswerRequest;
import com.trace.dto.InterviewStartRequest;
import com.trace.entity.InterviewRecord;
import reactor.core.publisher.Flux;
import java.util.Map;

public interface InterviewService {
    Map<String, Object> startInterview(Long userId, InterviewStartRequest request);
    Map<String, Object> submitAnswer(String sessionId, InterviewAnswerRequest request);
    Flux<String> startInterviewStream(Long userId, InterviewStartRequest request);
    Flux<String> submitAnswerStream(String sessionId, InterviewAnswerRequest request);
    IPage<InterviewRecord> getRecords(Long userId, int page, int size);
    InterviewRecord getRecordDetail(Long recordId);
    String generateReport(Long recordId);
}
