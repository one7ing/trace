package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.QuestionBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;
import java.util.Map;

/**
 * 题库 Mapper —— 随机抽题、按方向查询。
 */
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    /** 按方向随机取 N 道题 */
    @Select("SELECT * FROM question_bank WHERE topic = #{topic} ORDER BY RANDOM() LIMIT #{limit}")
    List<QuestionBank> findRandomByTopic(@Param("topic") String topic, @Param("limit") int limit);

    /** 不区分方向随机取 N 道题 */
    @Select("SELECT * FROM question_bank ORDER BY RANDOM() LIMIT #{limit}")
    List<QuestionBank> findRandom(@Param("limit") int limit);

    /** 获取所有方向 */
    @Select("SELECT DISTINCT topic FROM question_bank ORDER BY topic")
    List<String> findAllTopics();

    /** 按用户ID和topic查询题目 */
    @Select("SELECT * FROM question_bank WHERE user_id = #{userId} AND topic = #{topic} ORDER BY id")
    List<QuestionBank> findByUserIdAndTopic(@Param("userId") Long userId, @Param("topic") String topic);

    /** 按用户ID分组统计题库 */
    @Select("SELECT topic, COUNT(*) as cnt FROM question_bank WHERE user_id = #{userId} GROUP BY topic ORDER BY topic")
    List<Map<String, Object>> countByUserIdGroupByTopic(@Param("userId") Long userId);

    /** 删除用户指定题库的所有题目 */
    @Delete("DELETE FROM question_bank WHERE user_id = #{userId} AND topic = #{topic}")
    int deleteByUserIdAndTopic(@Param("userId") Long userId, @Param("topic") String topic);

    /** 从用户题库按 topic 随机取题 */
    @Select("SELECT * FROM question_bank WHERE user_id = #{userId} AND topic = #{topic} ORDER BY RANDOM() LIMIT #{limit}")
    List<QuestionBank> findRandomByUserIdAndTopic(@Param("userId") Long userId, @Param("topic") String topic, @Param("limit") int limit);
}
