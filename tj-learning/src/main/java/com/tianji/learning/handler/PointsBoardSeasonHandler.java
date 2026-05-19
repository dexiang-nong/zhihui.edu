package com.tianji.learning.handler;

import com.tianji.common.utils.DateUtils;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-04
 * @Description: 积分赛季创建
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsBoardSeasonHandler {
    
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    
    /**
     * 月初创建新赛季
     */
    @XxlJob("createPointsBoardSeasonTableJob")
    public void createPointsBoardSeason() {
        // 1. 获取当前日期
        LocalDate time = LocalDate.now();
        // 2. 查询当前赛季榜单
        PointsBoardSeason currentSeason = pointsBoardSeasonService.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time) // created_time <= #{time}
                .ge(PointsBoardSeason::getEndTime, time)   // end_time >= #{time}
                .one();
        if (currentSeason != null) {
            return; // 当前赛季榜单已经存在, 无需创建
        }
        // 3. 创建新赛季
        // 3.1. 查询最后一个赛季
        PointsBoardSeason lastSeason = pointsBoardSeasonService.lambdaQuery()
                .orderByDesc(PointsBoardSeason::getId)
                .last("limit 1")
                .one();
        // 3.2. 创建新赛季
        PointsBoardSeason season = new PointsBoardSeason();
        if (lastSeason == null) {
            season.setId(1);
            season.setName("第1赛季");
        } else {
            season.setId(lastSeason.getId() + 1);
            season.setName("第" + season.getId() + "赛季");
        }
        season.setBeginTime(DateUtils.getMonthBegin(time));
        season.setEndTime(DateUtils.getMonthEnd(time));
        pointsBoardSeasonService.save(season);
    }
}
