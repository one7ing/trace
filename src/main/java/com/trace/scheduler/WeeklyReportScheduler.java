package com.trace.scheduler;

import com.trace.mapper.UserMapper;
import com.trace.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 周报定时任务调度器。
 * 每周日凌晨 3:00 自动为所有用户生成周报。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final WeeklyReportService weeklyReportService;
    private final UserMapper userMapper;

    /** 每周日凌晨 3:00 触发，遍历所有用户生成周报 */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void generateAllReports() {
        List<Long> userIds = userMapper.selectAllIds();
        log.info("定时周报开始: 用户数={}", userIds.size());
        int success = 0, skip = 0, fail = 0;
        for (Long userId : userIds) {
            try {
                weeklyReportService.generateWeeklyReport(userId);
                success++;
            } catch (IllegalArgumentException e) {
                // 本周已生成过
                skip++;
            } catch (Exception e) {
                log.error("周报生成失败: userId={}", userId, e);
                fail++;
            }
        }
        log.info("定时周报完成: 成功={} 跳过={} 失败={}", success, skip, fail);
    }
}
