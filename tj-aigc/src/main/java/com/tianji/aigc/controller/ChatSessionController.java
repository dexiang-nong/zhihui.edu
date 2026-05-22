package com.tianji.aigc.controller;


import com.tianji.aigc.domain.vo.SessionVO;
import com.tianji.aigc.service.IChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 对话session 前端控制器
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-22
 */
@RestController
@RequestMapping("/session")
@Tag(name = "会话接口管理")
@RequiredArgsConstructor
public class ChatSessionController {
    
    private final IChatSessionService chatSessionService;
    
    /**
     * 新建会话
     */
    @Operation(summary = "新建会话")
    @PostMapping
    public SessionVO createSession(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return this.chatSessionService.createSession(num);
    }
    
    /**
     * 获取热门会话
     */
    @Operation(summary = "获取热门会话")
    @GetMapping("/hot")
    public List<SessionVO.Example> hotExamples(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return this.chatSessionService.hotExamples(num);
    }

}
