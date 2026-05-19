package com.tianji.learning.handler;

import cn.hutool.core.collection.CollUtil;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-04
 * @Description: 积分记录持久化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsRecordHandler {
    
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    
    private final IPointsRecordService pointsRecordService;
    
    private final TableInfoContext tableInfoContext;
    
    /**
     * 创建历史积分记录表: points_record_{seasonId}
     */
    @XxlJob("createPointsRecordTableJob")
    public void createPointsRecordTable() {
        // 1. 获取上月日期
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        // 2. 查询赛季id
        Integer seasonId = pointsBoardSeasonService.querySeasonIdByTime(lastMonth);
        if (seasonId == null) {
            return;
        }
        // 3. 创建数据库表
        pointsRecordService.createPointsRecordTableBySeason(seasonId);
    }
    
    /**
     * 持久化积分记录
     */
    @XxlJob("persistencePointsRecordJob")
    public void persistencePointsRecord() {
        // 1. 获取上月日期
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        // 2. 查询赛季id
        Integer seasonId = pointsBoardSeasonService.querySeasonIdByTime(lastMonth);
        // 3. 将动态表面存入ThreadLocal
        tableInfoContext.setInfo(LearningConstants.POINTS_RECORD_TABLE_PREFIX + seasonId);
        // 4. 定义分页参数
        int shardIndex = XxlJobHelper.getShardIndex(); // 获取执行器编号
        int shardTotal = XxlJobHelper.getShardTotal(); // 获取执行器数量
        int pageNo = shardIndex + 1; // 起始号就是分片序号+1
        int pageSize = 1000;
        while (true) {
            // 5. 分页查询积分记录数据 (Mysql)
            List<PointsRecord> list = pointsRecordService.queryPointsRecordList(lastMonth, pageNo, pageSize);
            if (CollUtil.isEmpty(list)) {
                break;
            }
            // 6. 将数据存入另一张表
            list.forEach(entity -> entity.setId(null));
            pointsRecordService.saveBatch(list);
            // 翻页
            pageNo += shardTotal;
        }
        // 7. 清空ThreadLocal
        tableInfoContext.remove();
    }
    
    /**
     * 清理积分记录表
     */
    @XxlJob("clearRedisPointsRecordJob")
    public void clearRedisPointsRecord() {
        // 1. 获取上月日期
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        // 2. 删除积分记录表
        LocalDateTime begin = DateUtils.getMonthBeginTime(lastMonth);
        LocalDateTime end = DateUtils.getMonthEndTime(lastMonth);
        pointsRecordService.lambdaUpdate()
                .between(PointsRecord::getCreateTime, begin, end)
                .remove();
    }
    
}
