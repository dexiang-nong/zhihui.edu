package com.tianji.remark.controller;


import com.tianji.api.dto.remark.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-24
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "点赞系统相关接口")
@RequestMapping("/likes")
public class LikedRecordController {
    
    private final ILikedRecordService likedRecordService;
    
    /**
     * 点赞或取消点赞
     */
    @Operation(summary = "点赞或取消点赞")
    @PostMapping
    public void addLikeRecord(@Valid @RequestBody LikeRecordFormDTO recordDTO) {
        likedRecordService.addLikeRecord(recordDTO);
    }
    
    /**
     * 批量查询点赞状态（查询当前用户是否点赞了指定的业务）
     */
    @Operation(summary = "批量查询点赞状态")
    @GetMapping("/list")
    public Set<Long> queryLikedStatus(@RequestParam("bizIds") List<Long> bizIds) {
        return likedRecordService.queryLikedStatus(bizIds);
    }
    
}
