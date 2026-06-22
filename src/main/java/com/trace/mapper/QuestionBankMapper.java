package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.QuestionBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}
