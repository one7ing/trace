package com.constant;

public interface constant {
    //redis取消key
    String CANCEL_KEY_PREFIX = "chat:cancel:";
    //短期记忆
    String REDIS_KEY_PREFIX = "chat:short:";
    interface memory{
        //会话记忆500条
        int MAX_CHAT_HISTORY = 500;
        //短期记忆20条
        int MAX_SHORT_TERM = 15;
        //长期记忆最长30条
        int MAX_MEMORIES = 30;
    }
    interface ChatMemoryExtract{
        String LOCK_KEY = "distributed:lock:chat-memory-extract";
        String LOCK_KEY_PREFIX = "chat:memory:extract:lock:";
        String LAST_EXTRACTED_KEY_PREFIX = "chat:last_extracted:";
        int EXTRACT_INTERVAL_MINUTES = 30;
        int RECENT_MESSAGE_COUNT = 15;
        int LOCK_TTL_SECONDS = 300;
    }
    interface Dashboard{
        int CHECKIN_WEIGHT = 4;
        int INTERVIEW_WEIGHT = 10;
        int PLAN_WEIGHT = 5;
        int CHECKIN_CAP = 40;
        int INTERVIEW_CAP = 30;
        int PLAN_CAP = 20;
        int DIARY_CAP = 10;
        int MAX_SCORE = 100;
        int HEATMAP_WEEKS = 12;
        int TREND_DAYS = 30;
        int DAY_CHECKIN_SCORE = 15;
        int DAY_INTERVIEW_SCORE = 25;
        int DAYS_PER_WEEK = 7;
    }
    interface Token{
        // Redis中refreshToken的key前缀
        String REFRESH_TOKEN_PREFIX = "trace:refresh:";
        // 短token（accessToken）过期时间：30分钟
        long ACCESS_EXPIRATION_MS = 30 * 60 * 1000L;
        // 长token（refreshToken）过期时间：7天
        long REFRESH_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;
        // accessToken过期错误码
        int CODE_ACCESS_EXPIRED = 40101;
        // refreshToken无效错误码
        int CODE_REFRESH_INVALID = 40102;
    }
    interface RabbitMQ{
        String PLAN_QUEUE = "trace.plan.generate";
        String PLAN_EXCHANGE = "trace.plan.exchange";
        String PLAN_ROUTING_KEY = "trace.plan.generate";

        String PRACTICE_JUDGE_QUEUE = "trace.practice.judge";
        String PRACTICE_JUDGE_EXCHANGE = "trace.practice.exchange";
        String PRACTICE_JUDGE_ROUTING_KEY = "trace.practice.judge";

        String MEMORY_EXTRACT_QUEUE = "trace.memory.extract";
        String MEMORY_EXTRACT_EXCHANGE = "trace.memory.exchange";
        String MEMORY_EXTRACT_ROUTING_KEY = "trace.memory.extract";
    }
}
