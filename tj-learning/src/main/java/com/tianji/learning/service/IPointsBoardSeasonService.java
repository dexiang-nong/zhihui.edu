package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoardSeason;

import java.time.LocalDate;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {
    
    /**
     * 根据时间查询当前赛季id
     */
    Integer querySeasonIdByTime(LocalDate time);
}
