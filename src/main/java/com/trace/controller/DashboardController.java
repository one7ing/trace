package com.trace.controller;

import com.trace.entity.DailyCheckIn;
import com.trace.entity.Diary;
import com.trace.entity.GrowthAnchor;
import com.trace.entity.InterviewRecord;
import com.trace.entity.LongTermMemory;
import com.trace.entity.StudyPlan;
import com.trace.mapper.DailyCheckInMapper;
import com.trace.mapper.DiaryMapper;
import com.trace.mapper.GrowthAnchorMapper;
import com.trace.mapper.InterviewRecordMapper;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.mapper.StudyPlanMapper;
import com.trace.dto.ApiResponse;
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

/**
 * 仪表盘控制器 —— 聚合首页所需的各类统计数据。
 * <p>
 * 提供两个核心接口：
 * <ul>
 *   <li>GET /api/dashboard —— 首页综合仪表盘（本周概览、面试趋势、打卡分布、日记摘要、活跃计划、近期记忆）</li>
 *   <li>GET /api/dashboard/growth —— 成长力数据（评分、趋势、热力图、记忆锚点）</li>
 * </ul>
 * 热力图模仿 GitHub 贡献图风格，展示最近 12 周的学习活跃度。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DiaryMapper diaryMapper;
    private final InterviewRecordMapper interviewMapper;
    private final StudyPlanMapper planMapper;
    private final LongTermMemoryMapper memoryMapper;
    private final DailyCheckInMapper checkInMapper;
    private final GrowthAnchorMapper anchorMapper;

    /** 成长力评分中每次打卡的基准分值 */
    private static final int CHECKIN_WEIGHT = 4;

    /** 成长力评分中每次面试的基准分值 */
    private static final int INTERVIEW_WEIGHT = 10;

    /** 成长力评分中每个计划的基准分值 */
    private static final int PLAN_WEIGHT = 5;

    /** 打卡分数上限 */
    private static final int CHECKIN_CAP = 40;

    /** 面试分数上限 */
    private static final int INTERVIEW_CAP = 30;

    /** 计划分数上限 */
    private static final int PLAN_CAP = 20;

    /** 日记分数上限 */
    private static final int DIARY_CAP = 10;

    /** 成长力总分上限 */
    private static final int MAX_SCORE = 100;

    /** 热力图统计周数 */
    private static final int HEATMAP_WEEKS = 12;

    /** 趋势图展示天数 */
    private static final int TREND_DAYS = 30;

    /** 每日得分中每次打卡的分值 */
    private static final int DAY_CHECKIN_SCORE = 15;

    /** 每日得分中每次面试的分值 */
    private static final int DAY_INTERVIEW_SCORE = 25;

    /** 每周的天数 */
    private static final int DAYS_PER_WEEK = 7;

    /**
     * 首页综合仪表盘数据。
     *
     * @param userId 当前登录用户 ID
     * @return 包含本周概览、面试趋势、打卡分布、日记摘要、活跃计划、近期记忆的综合数据
     */
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
        List<InterviewRecord> weekInterviews = interviewMapper
                .findByUserIdAndCompletedAtBetween(userId, weekStart, weekEnd);
        List<StudyPlan> allPlans = planMapper.findByUserIdOrderByCreatedAtDesc(userId);
        long inProgressPlans = allPlans.stream()
                .filter(p -> p.getPlanUrl() == null || p.getPlanUrl().isEmpty()
                        || "正在生成中...".equals(p.getPlanContent()))
                .count();

        Map<String, Object> weeklyOverview = new LinkedHashMap<>();
        weeklyOverview.put("diaryCount", weekDiaries.size());
        weeklyOverview.put("interviewCount", weekInterviews.size());
        weeklyOverview.put("planTotal", allPlans.size());
        weeklyOverview.put("planInProgress", inProgressPlans);
        data.put("weeklyOverview", weeklyOverview);

        // 2. 面试趋势（最近10次，按时间升序）
        List<InterviewRecord> recentInterviews = interviewMapper
                .findByUserIdOrderByCompletedAtDesc(userId);
        List<Map<String, Object>> interviewTrend = new ArrayList<>();
        int count = Math.min(recentInterviews.size(), 10);
        for (int i = count - 1; i >= 0; i--) {
            InterviewRecord ir = recentInterviews.get(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", ir.getCompletedAt() != null
                    ? ir.getCompletedAt().toLocalDate().toString() : "");
            point.put("score", ir.getAvgScore() != null
                    ? ir.getAvgScore().setScale(1, RoundingMode.HALF_UP).doubleValue() : 0);
            point.put("industry", ir.getIndustry() != null ? ir.getIndustry() : "");
            interviewTrend.add(point);
        }
        data.put("interviewTrend", interviewTrend);

        // 3. 本周打卡分布（扇形图用）
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

        // 5. 当前活跃计划（含打卡进度）
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

            // 打卡进度
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

        // 6. 长期记忆关键事件
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

    // ────────── 成长力 API ──────────

    /**
     * 获取综合成长力数据。
     * <p>
     * 包含成长力评分、环比变化、30 天趋势、12 周热力图和记忆锚点。
     * 热力图数据覆盖最近 12 周，综合统计每日打卡、面试和日记三种活动。
     *
     * @param userId 当前登录用户 ID
     * @return 成长力评分、趋势、热力图和锚点数据
     */
    @GetMapping("/growth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> growth(
            @AuthenticationPrincipal Long userId) {

        Map<String, Object> data = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        // ── 日期范围：本月 & 上月 ──
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);

        // ── 本月数据（用于评分计算） ──
        List<DailyCheckIn> monthChecks = checkInMapper.findByUserIdAndDateBetween(
                userId, monthStart, today);
        List<InterviewRecord> monthInterviews = interviewMapper
                .findByUserIdAndCompletedAtBetween(
                        userId, monthStart.atStartOfDay(), today.atTime(23, 59));
        List<DailyCheckIn> lastMonthChecks = checkInMapper.findByUserIdAndDateBetween(
                userId, lastMonthStart, lastMonthEnd);
        List<InterviewRecord> lastMonthInterviews = interviewMapper
                .findByUserIdAndCompletedAtBetween(
                        userId, lastMonthStart.atStartOfDay(), lastMonthEnd.atTime(23, 59));

        // ── 成长力评分算法 ──
        int checkins = monthChecks.size();
        int interviews = monthInterviews.size();
        int lastCheckins = lastMonthChecks.size();
        int lastInterviews = lastMonthInterviews.size();

        int score = Math.min(MAX_SCORE,
                Math.min(checkins * CHECKIN_WEIGHT, CHECKIN_CAP)
                        + Math.min(interviews * INTERVIEW_WEIGHT, INTERVIEW_CAP)
                        + Math.min(planMapper.findByUserIdOrderByCreatedAtDesc(userId).size()
                                * PLAN_WEIGHT, PLAN_CAP)
                        + Math.min(diaryMapper.findByUserIdOrderByCreatedAtDesc(userId).size(),
                                DIARY_CAP));
        data.put("growthScore", score);

        // 环比变化
        int lastScore = Math.min(MAX_SCORE,
                Math.min(lastCheckins * CHECKIN_WEIGHT, CHECKIN_CAP)
                        + Math.min(lastInterviews * INTERVIEW_WEIGHT, INTERVIEW_CAP));
        double momChange = lastScore > 0
                ? Math.round(((double) (score - lastScore) / lastScore) * 1000.0) / 10.0
                : 0;
        data.put("momChange", momChange);

        // ── 趋势数据（最近30天，每天一个分数点） ──
        List<Map<String, Object>> trend = buildTrendData(
                today, monthChecks, monthInterviews);
        data.put("trend", trend);

        // ── 热力图数据（最近 12 周，覆盖完整日期范围） ──
        LocalDate heatmapStart = today.minusWeeks(HEATMAP_WEEKS - 1)
                .with(DayOfWeek.MONDAY);
        List<DailyCheckIn> heatmapChecks = checkInMapper.findByUserIdAndDateBetween(
                userId, heatmapStart, today);
        List<InterviewRecord> heatmapInterviews = interviewMapper
                .findByUserIdAndCompletedAtBetween(
                        userId, heatmapStart.atStartOfDay(), today.atTime(23, 59));
        List<Diary> heatmapDiaries = diaryMapper.findByUserIdAndCreatedAtBetween(
                userId, heatmapStart.atStartOfDay(), today.atTime(23, 59));

        List<Map<String, Object>> heatmap = buildHeatmapData(
                today, heatmapChecks, heatmapInterviews, heatmapDiaries);
        data.put("heatmap", heatmap);

        // ── 锚点 ──
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

    /**
     * 构建最近 N 天的成长力趋势数据。
     *
     * @param today           当前日期
     * @param monthChecks     本月打卡记录
     * @param monthInterviews 本月面试记录
     * @return 按日期升序排列的每日分数列表
     */
    private List<Map<String, Object>> buildTrendData(
            LocalDate today,
            List<DailyCheckIn> monthChecks,
            List<InterviewRecord> monthInterviews) {

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDate dStart = d;
            LocalDate dEnd = d;
            long dayChecks = monthChecks.stream()
                    .filter(c -> !c.getCheckDate().isBefore(dStart)
                            && !c.getCheckDate().isAfter(dEnd))
                    .count();
            long dayInterviews = monthInterviews.stream()
                    .filter(ir -> ir.getCompletedAt() != null
                            && !ir.getCompletedAt().toLocalDate().isBefore(dStart)
                            && !ir.getCompletedAt().toLocalDate().isAfter(dEnd))
                    .count();
            int dayScore = (int) Math.min(MAX_SCORE,
                    dayChecks * DAY_CHECKIN_SCORE + dayInterviews * DAY_INTERVIEW_SCORE);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.toString());
            point.put("score", dayScore);
            trend.add(point);
        }
        return trend;
    }

    /**
     * 构建 GitHub 风格的热力图数据（最近 12 周，周一到周日）。
     * <p>
     * 每一天的活跃度计数 = 打卡次数 + 面试次数 + 日记篇数。
     * 数据按日期升序排列，前端可直接按周几分组渲染。
     *
     * @param today              当前日期
     * @param heatmapChecks      12 周范围内的打卡记录
     * @param heatmapInterviews  12 周范围内的面试记录
     * @param heatmapDiaries     12 周范围内的日记记录
     * @return 每个单元格包含 date（日期字符串）和 count（活跃计数）
     */
    private List<Map<String, Object>> buildHeatmapData(
            LocalDate today,
            List<DailyCheckIn> heatmapChecks,
            List<InterviewRecord> heatmapInterviews,
            List<Diary> heatmapDiaries) {

        List<Map<String, Object>> heatmap = new ArrayList<>();
        for (int w = HEATMAP_WEEKS - 1; w >= 0; w--) {
            for (int d = 0; d < DAYS_PER_WEEK; d++) {
                LocalDate date = today.minusWeeks(w)
                        .with(DayOfWeek.MONDAY)
                        .plusDays(d);
                if (date.isAfter(today)) {
                    continue;
                }
                LocalDate ds = date;
                LocalDate de = date;

                // 统计当日打卡次数
                long checkinCnt = heatmapChecks.stream()
                        .filter(ck -> !ck.getCheckDate().isBefore(ds)
                                && !ck.getCheckDate().isAfter(de))
                        .count();
                // 统计当日面试次数
                long interviewCnt = heatmapInterviews.stream()
                        .filter(ir -> ir.getCompletedAt() != null
                                && ir.getCompletedAt().toLocalDate().equals(date))
                        .count();
                // 统计当日日记篇数
                long diaryCnt = heatmapDiaries.stream()
                        .filter(diary -> diary.getCreatedAt() != null
                                && diary.getCreatedAt().toLocalDate().equals(date))
                        .count();

                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("date", date.toString());
                cell.put("count", (int) (checkinCnt + interviewCnt + diaryCnt));
                heatmap.add(cell);
            }
        }
        return heatmap;
    }

    /**
     * 添加一个成长记忆锚点。
     *
     * @param userId 当前登录用户 ID
     * @param body   包含 date（日期字符串，yyyy-MM-dd）和 label（标签文字）
     * @return 新创建的锚点对象
     */
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

    /**
     * 删除指定的成长记忆锚点。
     *
     * @param id 锚点 ID
     * @return 操作结果消息
     */
    @DeleteMapping("/anchor/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAnchor(
            @PathVariable Long id) {

        anchorMapper.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("已删除"));
    }
}
