package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.enums.PointsRecordType;
import com.tianji.learning.domain.po.PointsRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {
    
    /**
     * 查询今日用户在某类型下的积分
     */
    int queryUserPointsByTypeAndDate(@Param("userId") Long userId,
                                     @Param("type") PointsRecordType type,
                                     @Param("beginTime") LocalDateTime beginTime,
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询今日积分分类汇总
     */
    List<PointsRecord> queryTodayPoints(@Param("userId") Long userId,
                                        @Param("beginTime") LocalDateTime beginTime,
                                        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 创建数据库表：points_record_#{seasonId}
     */
    void createPointsRecordTableBySeason(@Param("tableName") String tableName);
}
