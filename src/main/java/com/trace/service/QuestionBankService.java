package com.trace.service;

import com.trace.entity.QuestionBank;
import java.util.List;

/**
 * 题库服务 —— 题库导入与随机抽题。
 */
public interface QuestionBankService {

    /** 从题库文件导入题目到 question_bank 表 */
    int importQuestionBank();

    /** 按方向随机取 N 题 */
    List<QuestionBank> getRandomQuestions(String topic, int count);

    /** 获取所有可用方向 */
    List<String> getAllTopics();
}
