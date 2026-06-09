package com.trace.scheduler;

import com.trace.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "distributed:lock:weekly-report";

    /**
     * 每周日凌晨 2:00 自动生成所有用户的成长周报
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void generateWeeklyReports() {
        RLock lock = redissonClient.getLock(LOCK_KEY);

        try {
            // 尝试获取分布式锁，等待 0 秒，持有 5 分钟自动释放
            if (lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                log.info("Starting weekly report generation task...");

                var users = userRepository.findAll();
                int successCount = 0;
                int failCount = 0;

                for (var user : users) {
                    try {
                        reportService.generateWeeklyReport(user.getId());
                        successCount++;
                    } catch (IllegalArgumentException e) {
                        // 本周周报已生成，跳过
                        log.debug("Weekly report already exists for user {}", user.getId());
                    } catch (Exception e) {
                        log.error("Failed to generate weekly report for user {}", user.getId(), e);
                        failCount++;
                    }
                }

                log.info("Weekly report task completed: {} success, {} failed", successCount, failCount);
            } else {
                log.info("Weekly report task skipped - another instance is running");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Weekly report task interrupted");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
