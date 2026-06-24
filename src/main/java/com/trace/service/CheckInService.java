package com.trace.service;

import java.util.Map;

public interface CheckInService {

    /** 今日打卡 */
    Map<String, Object> checkIn(Long userId, Long planId);

    /** 本周打卡状态 */
    Map<String, Object> weekStatus(Long userId, Long planId);

    /** 指定计划打卡进度 */
    Map<String, Object> progress(Long userId, Long planId);
}
