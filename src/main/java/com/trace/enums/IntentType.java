package com.trace.enums;

public enum IntentType {
    /** 直接回答：大模型自身可以回答，不查知识库也不联网 */
    DIRECT,

    /** 联网搜索：用户明确要求或需要实时信息 */
    WEB_SEARCH,

    /** 查用户知识库：用户要求基于自己的文档回答 */
    SEARCH
}
