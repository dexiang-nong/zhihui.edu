package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.service.IPointsRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "积分接口管理")
@RequestMapping("/points")
public class PointsRecordController {
    
    private final IPointsRecordService pointsRecordService;
    
    /**
     * 查询今日积分情况
     */
    @Operation(summary = "查询今日积分情况")
    @GetMapping("/today")
    public List<PointsStatisticsVO> queryTodayPoints() {
        return pointsRecordService.queryTodayPoints();
    }

}