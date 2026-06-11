package com.trace.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.entity.WeeklyReport;

public interface WeeklyReportService {
    WeeklyReport generateWeeklyReport(Long userId);
    IPage<WeeklyReport> list(Long userId, int page, int size);
    WeeklyReport getById(Long reportId);
}
