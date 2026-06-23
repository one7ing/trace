package com.trace.service;

import com.trace.entity.QuestionBank;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

/**
 * 题库服务 —— 题库导入与随机抽题 + 用户自建题库管理。
 */
public interface QuestionBankService {

    /** 从题库文件导入题目到 question_bank 表 */
    int importQuestionBank();

    /** 按方向随机取 N 题（系统题库） */
    List<QuestionBank> getRandomQuestions(String topic, int count);

    /** 获取所有可用方向 */
    List<String> getAllTopics();

    /** 创建用户题库并导入题目（文本内容），返回 {topic, count} */
    Map<String, Object> createBank(Long userId, String name, String content);

    /** 创建用户题库并导入题目（PDF文件），返回 {topic, count} */
    Map<String, Object> createBankFromFile(Long userId, String name, MultipartFile file);

    /** 列出用户题库 */
    List<Map<String, Object>> listBanks(Long userId);

    /** 删除用户题库及其下所有题目 */
    void deleteBank(Long userId, String topic);

    /** 向题库添加题目 */
    QuestionBank addQuestion(Long userId, String topic, String question, String referenceAnswer);

    /** 查看题库题目列表 */
    List<QuestionBank> listQuestions(Long userId, String topic);

    /** 删除单题 */
    void deleteQuestion(Long userId, Long questionId);

    /** 从用户题库按 topic 随机取题 */
    List<QuestionBank> getRandomFromBank(Long userId, String topic, int count);
}
