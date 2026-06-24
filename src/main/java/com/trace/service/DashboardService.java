package com.trace.service;

import java.util.Map;

public interface DashboardService {

    /** 综合仪表盘 */
    Map<String, Object> getDashboard(Long userId);

    /** 成长力数据 */
    Map<String, Object> getGrowth(Long userId);

    /** 添加成长锚点 */
    Map<String, Object> addAnchor(Long userId, String date, String label);

    /** 删除成长锚点 */
    void deleteAnchor(Long id);
}
