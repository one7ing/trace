package com.trace.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trace.agent.MemoryExtractAgent;
import com.trace.entity.*;
import com.trace.mapper.*;
import com.trace.service.MemoryService;
import com.trace.service.PdfService;
import com.trace.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private final ChatClient.Builder chatClientBuilder;
    private final MemoryService memoryService;
    private final MemoryExtractAgent memoryExtractAgent;
    private final PdfService pdfService;
    private final WeeklyReportMapper reportMapper;
    private final DiaryMapper diaryMapper;
    private final PracticeRecordMapper practiceRecordMapper;

    private static final String SYSTEM_PROMPT = """
            你是 Trace 系统的 AI 周报生成器。基于用户一周活动数据，客观生成成长周报。
            结构：概览、日记关键事件、知识探索热点、面试表现、下周建议。使用 Markdown 格式，全部中文。
            """;

    @Override
    @Transactional
    public WeeklyReport generateWeeklyReport(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate ws = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate we = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        if (reportMapper.findByUserIdAndWeekStart(userId, ws) != null)
            throw new IllegalArgumentException("本周周报已生成");

        List<Diary> diaries = diaryMapper.findByUserIdAndCreatedAtBetween(userId, ws.atStartOfDay(), we.atTime(LocalTime.MAX));
        List<PracticeRecord> practices = practiceRecordMapper.findByUserIdAndCompletedAtBetween(userId, ws.atStartOfDay(), we.atTime(LocalTime.MAX));
        List<LongTermMemory> mems = memoryService.getRecentMemories(userId, 20);

        StringBuilder ctx = new StringBuilder();
        ctx.append("## 本周日记（").append(diaries.size()).append("篇）\n");
        for (Diary d : diaries) ctx.append("- [").append(d.getCreatedAt().toLocalDate()).append("] ").append(d.getTitle()).append(" (").append(d.getMoodTag()).append(")\n");
        ctx.append("\n## 本周刷题（").append(practices.size()).append("次）\n");
        for (PracticeRecord pr : practices) ctx.append("- ").append(pr.getTopic()).append(" | 答对：").append(pr.getCorrectCount()).append("/").append(pr.getTotalQuestions()).append(" | 平均分：").append(pr.getScore()).append("\n");
        ctx.append("\n## 本周知识探索\n");
        for (LongTermMemory m : mems) ctx.append("- ").append(m.getContent()).append("\n");

        String content = chatClientBuilder.build().prompt().system(SYSTEM_PROMPT).user("请生成周报：\n" + ctx).call().content();
        String url = pdfService.generateAndUpload("成长周报_" + ws + "_" + we, content);
        WeeklyReport r = WeeklyReport.builder().userId(userId).weekStart(ws).weekEnd(we)
                .summary(content.length() > 200 ? content.substring(0, 200) + "..." : content)
                .fullContent(content).reportUrl(url).build();
        reportMapper.insert(r);

        // 周报生成后自动提取长期记忆
        try {
            int extracted = memoryExtractAgent.extractAndSave(
                    userId, content, "weekly_report");
            log.info("周报记忆提取完成: userId={}, 提取条数={}",
                    userId, extracted);
        } catch (Exception e) {
            log.warn("周报记忆提取失败: userId={}, 错误位置=WeeklyReportServiceImpl.generate",
                    userId, e);
        }

        return r;
    }

    @Override
    public IPage<WeeklyReport> list(Long userId, int page, int size) {
        List<WeeklyReport> all = reportMapper.findByUserIdOrderByWeekStartDesc(userId);
        Page<WeeklyReport> mp = new Page<>(page + 1, size);
        int s = (int) mp.offset(), e = Math.min(s + (int) mp.getSize(), all.size());
        mp.setRecords(all.subList(Math.min(s, all.size()), e)); mp.setTotal(all.size());
        return mp;
    }

    @Override
    public WeeklyReport getById(Long id) {
        WeeklyReport r = reportMapper.selectById(id);
        if (r == null) throw new IllegalArgumentException("周报不存在");
        return r;
    }
}
