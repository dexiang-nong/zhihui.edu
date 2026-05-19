package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.po.PointsBoard;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {
    
    /**
     * 根据赛季id创建积分排行榜数据库表
     */
    void createPointsBoardTableBySeason(@Param("tableName") String tableName);
}
