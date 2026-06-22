package com.trace.controller;

import com.trace.entity.PracticeRecord;
import com.trace.entity.DailyCheckIn;
import com.trace.entity.Diary;
import com.trace.entity.GrowthAnchor;
import com.trace.entity.LongTermMemory;
import com.trace.entity.StudyPlan;
import com.trace.mapper.DailyCheckInMapper;
import com.trace.mapper.DiaryMapper;
import com.trace.mapper.GrowthAnchorMapper;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.mapper.PracticeRecordMapper;
import com.trace.mapper.StudyPlanMapper;
import com.trace.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static com.constant.constant.Dashboard.*;

@Tag(name = "成长仪表盘", description = "综合数据面板、成长评分、热力图、趋势图、成长里程碑")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DiaryMapper diaryMapper;
    private final PracticeRecordMapper practiceMapper;
    private final StudyPlanMapper planMapper;
    private final LongTermMemoryMapper memoryMapper;
    private final DailyCheckInMapper checkInMapper;
    private final GrowthAnchorMapper anchorMapper;

    @Operation(summary = "综合仪表盘", description = "聚合本周概览、刷题趋势、打卡分布、日记摘要、活跃计划、近期记忆")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @AuthenticationPrincipal Long userId) {

        Map<String, Object> data = new LinkedHashMap<>();

        // 1. 本周概览
        LocalDateTime weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusDays(DAYS_PER_WEEK);

        List<Diary> weekDiaries = diaryMapper.findByUserIdAndCreatedAtBetween(
                userId, weekStart, weekEnd);
        List<PracticeRecord> weekPractices = practiceMapper
                .findByUserIdAndCompletedAtBetween(userId, weekStart, weekEnd);
        List<StudyPlan> allPlans = planMapper.findByUserIdOrderByCreatedAtDesc(userId);
        long inProgressPlans = allPlans.stream()
                .filter(p -> p.getPlanUrl() == null || p.getPlanUrl().isEmpty()
                        || "正在生成中...".equals(p.getPlanContent()))
                .count();

        Map<String, Object> weeklyOverview = new LinkedHashMap<>();
        weeklyOverview.put("diaryCount", weekDiaries.size());
        weeklyOverview.put("practiceCount", weekPractices.size());
        weeklyOverview.put("planTotal", allPlans.size());
        weeklyOverview.put("planInProgress", inProgressPlans);
        data.put("weeklyOverview", weeklyOverview);

        // 2. 刷题趋势（最近10次，按时间升序）
        List<PracticeRecord> recentPractices = practiceMapper
                .findByUserIdOrderByCompletedAtDesc(userId);
        List<Map<String, Object>> practiceTrend = new ArrayList<>();
        int count = Math.min(recentPractices.size(), 10);
        for (int i = count - 1; i >= 0; i--) {
            PracticeRecord pr = recentPractices.get(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", pr.getCompletedAt() != null
                    ? pr.getCompletedAt().toLocalDate().toString() : "");
            point.put("score", pr.getScore() != null
                    ? pr.getScore().setScale(1, RoundingMode.HALF_UP).doubleValue() : 0);
            point.put("topic", pr.getTopic() != null ? pr.getTopic() : "");
            practiceTrend.add(point);
        }
        data.put("practiceTrend", practiceTrend);

        // 3. 本周打卡分布
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<DailyCheckIn> weekChecks = checkInMapper
                .findByUserIdAndDateBetween(userId, weekStart.toLocalDate(), weekEnd.toLocalDate());
        Set<String> checkedDates = weekChecks.stream()
                .map(c -> c.getCheckDate().toString())
                .collect(Collectors.toSet());

        Map<String, Boolean> checkinDistribution = new LinkedHashMap<>();
        for (int d = 0; d < DAYS_PER_WEEK; d++) {
            LocalDate day = weekStart.toLocalDate().plusDays(d);
            checkinDistribution.put(dayNames[d], checkedDates.contains(day.toString()));
        }
        data.put("checkinDistribution", checkinDistribution);

        // 4. 最近日记摘要
        List<Diary> recentDiaries = diaryMapper.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> diarySummaries = recentDiaries.stream()
                .limit(3)
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", d.getId());
                    m.put("title", d.getTitle());
                    m.put("moodTag", d.getMoodTag() != null ? d.getMoodTag() : "");
                    m.put("createdAt", d.getCreatedAt() != null
                            ? d.getCreatedAt().toString() : "");
                    return m;
                })
                .collect(Collectors.toList());
        data.put("recentDiaries", diarySummaries);

        // 5. 活跃计划
        StudyPlan activePlan = allPlans.stream()
                .filter(p -> p.getPlanUrl() != null && !p.getPlanUrl().isEmpty()
                        && !"正在生成中...".equals(p.getPlanContent()))
                .findFirst().orElse(null);
        Map<String, Object> planInfo = new LinkedHashMap<>();
        if (activePlan != null) {
            planInfo.put("id", activePlan.getId());
            planInfo.put("goal", activePlan.getGoal());
            planInfo.put("totalDuration", activePlan.getTotalDuration() != null
                    ? activePlan.getTotalDuration() : 0);
            planInfo.put("createdAt", activePlan.getCreatedAt() != null
                    ? activePlan.getCreatedAt().toString() : "");
            long planChecked = weekChecks.stream()
                    .filter(c -> c.getPlanId() != null
                            && c.getPlanId().equals(activePlan.getId()))
                    .count();
            planInfo.put("checkedCount", (int) planChecked);
        } else {
            planInfo.put("goal", "");
            planInfo.put("totalDuration", 0);
            planInfo.put("checkedCount", 0);
        }
        data.put("activePlan", planInfo);

        // 6. 长期记忆
        List<LongTermMemory> memories = memoryMapper.findRecentByUserId(userId, 5);
        List<Map<String, Object>> memoryEvents = memories.stream()
                .map(m -> {
                    Map<String, Object> mm = new LinkedHashMap<>();
                    mm.put("content", m.getContent());
                    mm.put("sourceType", m.getSourceType() != null ? m.getSourceType() : "");
                    mm.put("createdAt", m.getCreatedAt() != null
                            ? m.getCreatedAt().toString() : "");
                    return mm;
                })
                .collect(Collectors.toList());
        data.put("recentMemories", memoryEvents);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "成长力数据", description = "成长评分、环比变化、30天趋势、12周热力图、成长锚点")
    @GetMapping("/growth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> growth(
            @AuthenticationPrincipal Long userId) {

        Map<String, Object> data = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);

        List<DailyCheckIn> monthChecks = checkInMapper.findByUserIdAndDateBetween(
                userId, monthStart, today);
        List<PracticeRecord> monthPractices = practiceMapper
                .findByUserIdAndCompletedAtBetween(
                        userId, monthStart.atStartOfDay(), today.atTime(23, 59));
        List<DailyCheckIn> lastMonthChecks = checkInMapper.findByUserIdAndDateBetween(
                userId, lastMonthStart, lastMonthEnd);
        List<PracticeRecord> lastMonthPractices = practiceMapper
                .findByUserIdAndCompletedAtBetween(
                        userId, lastMonthStart.atStartOfDay(), lastMonthEnd.atTime(23, 59));

        int checkins = monthChecks.size();
        int practices = monthPractices.size();
        int lastCheckins = lastMonthChecks.size();
        int lastPractices = lastMonthPractices.size();

        int score = Math.min(MAX_SCORE,
                Math.min(checkins * CHECKIN_WEIGHT, CHECKIN_CAP)
                        + Math.min(practices * INTERVIEW_WEIGHT, INTERVIEW_CAP)
                        + Math.min(planMapper.findByUserIdOrderByCreatedAtDesc(userId).size()
                                * PLAN_WEIGHT, PLAN_CAP)
                        + Math.min(diaryMapper.findByUserIdOrderByCreatedAtDesc(userId).size(),
                                DIARY_CAP));
        data.put("growthScore", score);

        int lastScore = Math.min(MAX_SCORE,
                Math.min(lastCheckins * CHECKIN_WEIGHT, CHECKIN_CAP)
                        + Math.min(lastPractices * INTERVIEW_WEIGHT, INTERVIEW_CAP));
        double momChange = lastScore > 0
                ? Math.round(((double) (score - lastScore) / lastScore) * 1000.0) / 10.0
                : 0;
        data.put("momChange", momChange);

        List<Map<String, Object>> trend = buildTrendData(today, monthChecks, monthPractices);
        data.put("trend", trend);

        LocalDate heatmapStart = today.minusWeeks(HEATMAP_WEEKS - 1)
                .with(DayOfWeek.MONDAY);
        List<DailyCheckIn> heatmapChecks = checkInMapper.findByUserIdAndDateBetween(
                userId, heatmapStart, today);
        List<PracticeRecord> heatmapPractices = practiceMapper
                .findByUserIdAndCompletedAtBetween(
                        userId, heatmapStart.atStartOfDay(), today.atTime(23, 59));
        List<Diary> heatmapDiaries = diaryMapper.findByUserIdAndCreatedAtBetween(
                userId, heatmapStart.atStartOfDay(), today.atTime(23, 59));

        List<Map<String, Object>> heatmap = buildHeatmapData(
                today, heatmapChecks, heatmapPractices, heatmapDiaries);
        data.put("heatmap", heatmap);

        List<GrowthAnchor> anchors = anchorMapper.findByUserId(userId);
        data.put("anchors", anchors.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("date", a.getAnchorDate().toString());
            m.put("label", a.getLabel());
            return m;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private List<Map<String, Object>> buildTrendData(
            LocalDate today,
            List<DailyCheckIn> monthChecks,
            List<PracticeRecord> monthPractices) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long dayChecks = monthChecks.stream()
                    .filter(c -> !c.getCheckDate().isBefore(d)
                            && !c.getCheckDate().isAfter(d))
                    .count();
            long dayPractices = monthPractices.stream()
                    .filter(pr -> pr.getCompletedAt() != null
                            && !pr.getCompletedAt().toLocalDate().isBefore(d)
                            && !pr.getCompletedAt().toLocalDate().isAfter(d))
                    .count();
            int dayScore = (int) Math.min(MAX_SCORE,
                    dayChecks * DAY_CHECKIN_SCORE + dayPractices * DAY_INTERVIEW_SCORE);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.toString());
            point.put("score", dayScore);
            trend.add(point);
        }
        return trend;
    }

    private List<Map<String, Object>> buildHeatmapData(
            LocalDate today,
            List<DailyCheckIn> heatmapChecks,
            List<PracticeRecord> heatmapPractices,
            List<Diary> heatmapDiaries) {
        List<Map<String, Object>> heatmap = new ArrayList<>();
        for (int w = HEATMAP_WEEKS - 1; w >= 0; w--) {
            for (int d = 0; d < DAYS_PER_WEEK; d++) {
                LocalDate date = today.minusWeeks(w)
                        .with(DayOfWeek.MONDAY).plusDays(d);
                if (date.isAfter(today)) continue;

                long checkinCnt = heatmapChecks.stream()
                        .filter(ck -> !ck.getCheckDate().isBefore(date)
                                && !ck.getCheckDate().isAfter(date))
                        .count();
                long practiceCnt = heatmapPractices.stream()
                        .filter(pr -> pr.getCompletedAt() != null
                                && pr.getCompletedAt().toLocalDate().equals(date))
                        .count();
                long diaryCnt = heatmapDiaries.stream()
                        .filter(diary -> diary.getCreatedAt() != null
                                && diary.getCreatedAt().toLocalDate().equals(date))
                        .count();

                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("date", date.toString());
                cell.put("count", (int) (checkinCnt + practiceCnt + diaryCnt));
                heatmap.add(cell);
            }
        }
        return heatmap;
    }

    @PostMapping("/anchor")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addAnchor(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        GrowthAnchor a = GrowthAnchor.builder()
                .userId(userId)
                .anchorDate(LocalDate.parse(body.get("date")))
                .label(body.get("label"))
                .createdAt(LocalDateTime.now())
                .build();
        anchorMapper.insert(a);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", a.getId());
        r.put("date", a.getAnchorDate().toString());
        r.put("label", a.getLabel());
        return ResponseEntity.ok(ApiResponse.success(r));
    }

    @DeleteMapping("/anchor/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAnchor(@PathVariable Long id) {
        anchorMapper.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("已删除"));
    }
}
