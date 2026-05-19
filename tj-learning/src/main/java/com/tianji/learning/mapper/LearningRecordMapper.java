package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.learning.domain.po.LearningRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {
    
    List<IdAndNumDTO> countLearnedSections(@Param("userId") Long userId,
                                           @Param("begin") LocalDateTime begin,
                                           @Param("end") LocalDateTime end);
    
    Integer queryTotalPlan(Long userId);
}
