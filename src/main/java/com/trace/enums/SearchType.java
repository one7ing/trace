package com.trace.enums;

/**
 * 检索类型枚举。
 */
public enum SearchType {

    /** 纯向量语义检索 */
    SEMANTIC,

    /** 混合检索（向量 0.7 + 全文 0.3） */
    HYBRID,

    /** 关键词全文检索 */
    KEYWORD
}
