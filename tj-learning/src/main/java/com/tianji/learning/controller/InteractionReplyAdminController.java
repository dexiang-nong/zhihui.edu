package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-23
 * @Description: 管理端互动回答接口管理
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "管理端互动回答接口管理")
@RequestMapping("/admin/replies")
public class InteractionReplyAdminController {
    
    private final IInteractionReplyService replyService;
    
    /**
     * 管理端分页查询回答或评论列表
     */
    @Operation(summary = "管理端分页查询回答或评论列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryAdminReplyPage(ReplyPageQuery query) {
        return replyService.queryAdminReplyPage(query);
    }
    
    /**
     * 管理端显示或隐藏评论
     */
    @Operation(summary = "管理端显示或隐藏评论")
    @PutMapping("/{id}/hidden/{hidden}")
    public void showOrHiddenReply(@PathVariable Long id, @PathVariable boolean hidden) {
        replyService.showOrHiddenReply(id, hidden);
    }
    
    /**
     * 管理端根据id查询回答详情
     */
    @Operation(summary = "管理端根据id查询回答详情")
    @GetMapping("/{id}")
    public ReplyVO queryAdminReplyById(@PathVariable Long id) {
        return replyService.queryAdminReplyById(id);
    }
}
