package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
public interface IPointsBoardService extends IService<PointsBoard> {
    
    /**
     * 分页查询指定赛季的积分排行榜
     */
    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);
    
    /**
     * 根据赛季id创建积分排行榜数据库表
     */
    void createPointsBoardTableBySeason(Integer seasonId);
    
    /**
     * 分页查询积分排行榜列表
     */
    List<PointsBoard> queryCurrentPointsBoardList(String key, int pageNo, int pageSize);
}
