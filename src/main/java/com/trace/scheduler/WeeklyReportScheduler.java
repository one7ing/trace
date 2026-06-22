package com.trace.scheduler;

import com.trace.mapper.UserMapper;
import com.trace.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {
    private final WeeklyReportService reportService;
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;
    private static final String LOCK_KEY = "distributed:lock:weekly-report";

    @Scheduled(cron = "0 0 2 * * SUN")
    public void generateWeeklyReports() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                log.info("开始执行周报定时任务...");
                var users = userMapper.selectList(null);
                int ok = 0, fail = 0;
                for (var u : users) {
                    try { reportService.generateWeeklyReport(u.getId()); ok++; }
                    catch (IllegalArgumentException e) { log.debug("用户{}周报已存在", u.getId()); }
                    catch (Exception e) { log.error("用户{}周报生成失败: WeeklyReportScheduler", u.getId(), e); fail++; }
                }
                log.info("周报定时任务完成: 成功{}，失败{}", ok, fail);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }
}
