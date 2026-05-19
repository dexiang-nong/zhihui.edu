package com.tianji.learning.controller;


import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "排行榜接口管理")
@RequestMapping("/boards")
public class PointsBoardController {
    
    private final IPointsBoardService pointsBoardService;
    
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    
    /**
     * 分页查询指定赛季的积分排行榜
     */
    @Operation(summary = "分页查询指定赛季的积分排行榜")
    @GetMapping
    public PointsBoardVO queryCurrentBoard(PointsBoardQuery query) {
        return pointsBoardService.queryPointsBoardBySeason(query);
    }
    
    /**
     * 查询赛季列表
     */
    @Operation(summary = "查询赛季列表")
    @GetMapping("/seasons/list")
    public List<PointsBoardSeason> querySeasons() {
        return pointsBoardSeasonService.lambdaQuery().list();
    }
    
}
