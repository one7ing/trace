package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.GrowthAnchor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GrowthAnchorMapper extends BaseMapper<GrowthAnchor> {

    @Select("SELECT * FROM growth_anchors WHERE user_id = #{userId} ORDER BY anchor_date DESC")
    List<GrowthAnchor> findByUserId(Long userId);
}
