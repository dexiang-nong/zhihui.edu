package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "互动回答接口管理")
@RequestMapping("/replies")
public class InteractionReplyController {
    
    private final IInteractionReplyService replyService;
    
    /**
     * 新增回答或评论
     */
    @Operation(summary = "新增回答或评论")
    @PostMapping
    public void addReply(@Valid @RequestBody ReplyDTO replyDTO) {
        replyService.addReply(replyDTO);
    }
    
    /**
     * 分页查询回答或评论列表
     */
    @Operation(summary = "分页查询回答或评论列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {
        return replyService.queryReplyPage(query);
    }

}
