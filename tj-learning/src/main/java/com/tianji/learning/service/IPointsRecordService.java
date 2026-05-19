package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.enums.PointsRecordType;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
public interface IPointsRecordService extends IService<PointsRecord> {
    
    /**
     * 保存学习积分记录
     */
    void addPointsRecord(Long userId, Integer points, PointsRecordType pointsRecordType);
    
    /**
     * 查询今日积分情况
     */
    List<PointsStatisticsVO> queryTodayPoints();
    
    /**
     * 创建数据库表：points_record_#{seasonId}
     */
    void createPointsRecordTableBySeason(Integer seasonId);
    
    /**
     * 分页查询积分记录数据
     */
    List<PointsRecord> queryPointsRecordList(LocalDate time, int pageNo, int pageSize);
}
