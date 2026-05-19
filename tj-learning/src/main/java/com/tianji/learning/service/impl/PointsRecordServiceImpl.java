package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.enums.PointsRecordType;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {
    
    private final PointsRecordMapper pointsRecordMapper;
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 保存学习积分记录
     */
    @Override
    public void addPointsRecord(Long userId, Integer points, PointsRecordType type) {
        /*
                                        积分获取规则
            1. 签到规则
                连续7天奖励10分  连续14天 奖励20  连续28天奖励40分， 每月签到进度当月第一天重置
            
            2. 学习规则
                每学习一小节，积分+10，每天获得上限50分
            
            3. 交互规则（有效交互数据参与积分规则，无效数据会被删除）
                - 写评价 积分+10
                - 写问答 积分+5 每日获得上限为20分
                - 写笔记 积分+3 每次被采集+2 每日获得上限为20分
         */
        // 1. 校验是否有资格添加积分记录
        int maxPoints = type.getMaxPoints();
        LocalDateTime now = LocalDateTime.now();
        if (maxPoints > 0) {
            // 1.1. 查询今日该类型已获取积分
            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
            int pointsSum = pointsRecordMapper.queryUserPointsByTypeAndDate(userId, type, begin, end);
            // 1.2. 判断今日积分是否达到上限
            if (pointsSum >= maxPoints) {
                return;
            }
            // 1.3. 判断获取的积分添加后是否会超过上限，如果超过了，则只保存上限的积分
            if (pointsSum + points > maxPoints) {
                points = maxPoints - pointsSum;
            }
        }
        // 2. 保存学习积分记录
        PointsRecord entity = new PointsRecord();
        entity.setUserId(userId);
        entity.setPoints(points);
        entity.setType(type);
        save(entity);
        // 3. 累加积分到Redis ( boards:{yyyyMM} )
        String yyyyMM = now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + yyyyMM;
        redisTemplate.opsForZSet().incrementScore(key, userId.toString(), points);
    }
    
    /**
     * 查询今日积分情况
     */
    @Override
    public List<PointsStatisticsVO> queryTodayPoints() {
        // 1. 查询今日各分类积分总和
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        List<PointsRecord> list = pointsRecordMapper.queryTodayPoints(userId, begin, end);
        if (CollectionUtil.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 3. 遍历List，根据类型设置相应信息
        List<PointsStatisticsVO> voList = new ArrayList<>(list.size());
        for (PointsRecord entity : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            voList.add(vo);
            vo.setPoints(entity.getPoints());
            vo.setType(entity.getType().getDesc());
            vo.setMaxPoints(entity.getType().getMaxPoints());
        }
        return voList;
    }
    
    /**
     * 创建数据库表：points_record_#{seasonId}
     */
    @Override
    public void createPointsRecordTableBySeason(Integer seasonId) {
        String tableName = LearningConstants.POINTS_RECORD_TABLE_PREFIX + seasonId;
        pointsRecordMapper.createPointsRecordTableBySeason(tableName);
    }
    
    /**
     * 分页查询积分记录数据
     */
    @Override
    public List<PointsRecord> queryPointsRecordList(LocalDate time, int pageNo, int pageSize) {
        LocalDateTime beginOfMonth = DateUtils.getMonthBeginTime(time);
        LocalDateTime endOfMonth = DateUtils.getMonthEndTime(time);
        Page<PointsRecord> page = lambdaQuery()
                .between(PointsRecord::getCreateTime, beginOfMonth, endOfMonth)
                .page(new Page<>(pageNo, pageSize));
        return page.getRecords();
    }
}
