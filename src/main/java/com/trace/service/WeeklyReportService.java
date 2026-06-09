package com.trace.service;

import com.trace.entity.Diary;
import com.trace.entity.InterviewRecord;
import com.trace.entity.LongTermMemory;
import com.trace.entity.WeeklyReport;
import com.trace.repository.DiaryRepository;
import com.trace.repository.InterviewRecordRepository;
import com.trace.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final ChatClient.Builder chatClientBuilder;
    private final MemoryService memoryService;
    private final PdfService pdfService;
    private final WeeklyReportRepository reportRepository;
    private final DiaryRepository diaryRepository;
    private final InterviewRecordRepository interviewRecordRepository;

    private static final String SYSTEM_PROMPT = """
            你是 Trace 系统的 AI 周报生成器。你的职责是：
            1. 基于用户一周的活动数据，客观生成成长周报
            2. 结构包括：概览、日记关键事件、知识探索热点、面试表现、下周建议
            3. 语气积极但不浮夸，基于数据说话
            4. 使用 Markdown 格式输出
            """;

    /**
     * 生成用户周报（由定时任务或手动调用）
     */
    @Transactional
    public WeeklyReport generateWeeklyReport(Long userId) {
        // 计算本周范围（周一到周日）
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 检查是否已生成
        reportRepository.findByUserIdAndWeekStart(userId, weekStart).ifPresent(existing -> {
            throw new IllegalArgumentException("本周周报已生成");
        });

        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.atTime(LocalTime.MAX);

        // 检索本周数据
        List<Diary> diaries = diaryRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                userId, startDateTime, endDateTime);
        List<InterviewRecord> interviews = interviewRecordRepository
                .findByUserIdAndCompletedAtBetweenOrderByCompletedAtDesc(userId, startDateTime, endDateTime);
        List<LongTermMemory> knowledgeMemories = memoryService.getRecentMemories(
                userId, List.of("knowledge"), 20);

        // 构建 Prompt
        StringBuilder dataContext = new StringBuilder();
        dataContext.append("## 本周日记记录（").append(diaries.size()).append("篇）\n");
        for (Diary d : diaries) {
            dataContext.append("- [").append(d.getCreatedAt().toLocalDate()).append("] ")
                    .append(d.getTitle()).append("（心情：").append(d.getMoodTag()).append("）\n");
        }

        dataContext.append("\n## 本周面试记录（").append(interviews.size()).append("次）\n");
        for (InterviewRecord ir : interviews) {
            dataContext.append("- ").append(ir.getIndustry()).append(" | 技能：")
                    .append(String.join(", ", ir.getSkillTags()))
                    .append(" | 平均分：").append(ir.getAvgScore()).append("\n");
        }

        dataContext.append("\n## 本周知识探索\n");
        for (LongTermMemory m : knowledgeMemories) {
            dataContext.append("- ").append(m.getContent()).append("\n");
        }

        // 调用大模型生成周报
        ChatClient chatClient = chatClientBuilder.build();
        String fullContent = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("请根据以下本周数据生成成长周报：\n\n" + dataContext.toString())
                .call()
                .content();

        // 生成摘要（取前200字）
        String summary = fullContent.length() > 200 ? fullContent.substring(0, 200) + "..." : fullContent;

        // 生成 PDF
        String reportUrl = pdfService.generateAndUpload(
                "成长周报_" + weekStart + "_" + weekEnd, fullContent);

        // 保存记录
        WeeklyReport report = WeeklyReport.builder()
                .userId(userId)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .summary(summary)
                .fullContent(fullContent)
                .reportUrl(reportUrl)
                .build();

        report = reportRepository.save(report);
        log.info("Weekly report generated: userId={}, weekStart={}", userId, weekStart);

        return report;
    }

    public Page<WeeklyReport> list(Long userId, Pageable pageable) {
        return reportRepository.findByUserIdOrderByWeekStartDesc(userId, pageable);
    }

    public WeeklyReport getById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("周报不存在"));
    }
}
