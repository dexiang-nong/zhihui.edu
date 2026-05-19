package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-21
 * @Description: 管理端互动提问接口管理
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "管理端互动提问接口管理")
@RequestMapping("/admin/questions")
public class InteractionQuestionAdminController {
    
    private final IInteractionQuestionService questionService;
    
    /**
     * 管理端分页查询问题
     */
    @Operation(summary = "管理端分页查询问题")
    @GetMapping("/page")
    public PageDTO<QuestionAdminVO> queryAdminQuestionPage(QuestionAdminPageQuery query) {
        return questionService.queryAdminQuestionPage(query);
    }
    
    /**
     * 管理端隐藏或显示问题
     */
    @Operation(summary = "管理端隐藏或显示问题")
    @PutMapping("/{id}/hidden/{hidden}")
    public void showOrHiddenQuestion(@PathVariable Long id, @PathVariable boolean hidden) {
        questionService.showOrHiddenQuestion(id, hidden);
    }
    
    /**
     * 管理端根据id查询问题详情
     */
    @Operation(summary = "管理端根据id查询问题详情")
    @GetMapping("/{id}")
    public QuestionAdminVO queryAdminQuestionById(@PathVariable Long id) {
        return questionService.queryAdminQuestionById(id);
    }
    
}
