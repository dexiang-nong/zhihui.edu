package com.tianji.learning.handler;

import cn.hutool.core.collection.CollUtil;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-01
 * @Description: 对Redis积分排行榜作持久化
 */
@Component
@RequiredArgsConstructor
public class PointsBoardHandler {
    
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    
    private final IPointsBoardService pointsBoardService;
    
    private final StringRedisTemplate redisTemplate;
    
    private final TableInfoContext tableInfoContext;
    
    /**
     * 创建积分排行榜数据库表: points_board_{seasonId}
     */
    @XxlJob("createPointsBoardTableJob")
    public void PointsBoardTable() {
        // 1. 获取上月日期
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        // 2. 查询赛季id
        Integer seasonId = pointsBoardSeasonService.querySeasonIdByTime(lastMonth);
        if (seasonId == null) {
            return;
        }
        // 3. 创建数据库表
        pointsBoardService.createPointsBoardTableBySeason(seasonId);
    }
    
    /**
     * 持久化积分排行榜
     */
    @XxlJob("persistencePointsBoardJob")
    public void persistencePointsBoard() {
        // 1. 获取上月时间
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        // 2. 计算动态表面
        // 2.1. 查询赛季ID
        Integer seasonId = pointsBoardSeasonService.querySeasonIdByTime(lastMonth);
        // 2.2. 将表名存入ThreadLocal
        tableInfoContext.setInfo(LearningConstants.POINTS_BOARD_TABLE_PREFIX + seasonId);
        // 3. 拼接key
        String yyyyMM = lastMonth.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + yyyyMM;
        // 4. 定义分页参数
        int shardIndex = XxlJobHelper.getShardIndex(); // 获取执行器编号
        int shardTotal = XxlJobHelper.getShardTotal(); // 获取执行器数量
        int pageNo = shardIndex + 1; // 起始号就是分片序号+1
        int pageSize = 1000;
        while (true) {
            // 5. 分页查询积分排行榜数据
            List<PointsBoard> list = pointsBoardService.queryCurrentPointsBoardList(key, pageNo, pageSize);
            if (CollUtil.isEmpty(list)) {
                break;
            }
            // 6. 数据持久化
            // 6.1. id作为排名
            list.forEach(entity -> {
                entity.setId(entity.getRank().longValue());
                entity.setRank(null);
            });
            // 6.2. 持久化
            pointsBoardService.saveBatch(list);
            // 6.3. 翻页
            pageNo += shardTotal; // 就是翻过N个分片数量
        }
        // 7. 删除动态表名
        tableInfoContext.remove();
    }
    
    /**
     * 清理积分排行榜
     */
    @XxlJob("clearRedisPointsBoardJob")
    public void clearPointsBoard() {
        // 1. 拼接KEY
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        String yyyyMM = lastMonth.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + yyyyMM;
        /*
            2. 删除KEY
            2.1. delete: 同步阻塞操作, 当删除一个大对象时, 可能导致Redis短暂阻塞, 影响其他操作的执行
            2.2. unlink: 异步阻塞操作, Redis将删除操作交给后台线程执行, 不阻塞主线程, 适合删除大对象
         */
//        redisTemplate.delete(key);
        redisTemplate.unlink(key);
    }
    
}
