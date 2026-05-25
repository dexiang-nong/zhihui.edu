package com.tianji.aigc.controller;


import com.tianji.aigc.domain.vo.MessageVO;
import com.tianji.aigc.domain.vo.SessionVO;
import com.tianji.aigc.service.IChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    
    @Operation(summary = "新建会话")
    @PostMapping
    public SessionVO createSession(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return this.chatSessionService.createSession(num);
    }
    
    @Operation(summary = "获取热门会话")
    @GetMapping("/hot")
    public List<SessionVO.Example> hotExamples(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return this.chatSessionService.hotExamples(num);
    }
    
    @Operation(summary = "查询单个历史对话详情")
    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return chatSessionService.queryBySessionId(sessionId);
    }
    
    @Operation(summary = "查询历史会话")
    @GetMapping("/history")
    public Map<String, List<SessionVO>> queryHistorySession() {
        return chatSessionService.queryHistorySession();
    }
    
    @Operation(summary = "更新历史会话标题")
    @PutMapping("/history")
    public void updateHistorySessionTitle(@RequestParam("sessionId") String sessionId,
                                       @RequestParam("title") String title) {
        chatSessionService.updateHistorySessionTitle(sessionId, title);
    }
    
    @Operation(summary = "删除历史会话")
    @DeleteMapping("/history")
    public void deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        chatSessionService.deleteHistorySession(sessionId);
    }

}
