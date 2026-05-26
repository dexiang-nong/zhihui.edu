package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.AIService;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "互动提问接口管理")
@RequestMapping("/questions")
public class InteractionQuestionController {
    
    private final IInteractionQuestionService questionService;
    
    private final AIService aiService;
    
    /**
     * 新增互动问题
     */
    @Operation(summary = "新增互动问题")
    @PostMapping
    public void addQuestion(@Valid @RequestBody QuestionFormDTO questionFormDTO) {
        InteractionQuestion interactionQuestion = questionService.addQuestion(questionFormDTO);
        
        aiService.autoReply(interactionQuestion);
    }
    
    /**
     * 修改互动问题
     */
    @Operation(summary ="修改互动问题")
    @PutMapping("/{id}")
    public void updateQuestion(@PathVariable("id") Long id, @Valid @RequestBody QuestionFormDTO questionFormDTO) {
        questionService.updateQuestion(id, questionFormDTO);
    }
    
    /**
     * 用户端分页查询问题
     */
    @Operation(summary ="用户端分页查询问题")
    @GetMapping("/page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        return questionService.queryQuestionPage(query);
    }
    
    /**
     * 根据id查询问题详情
     */
    @Operation(summary ="根据id查询问题详情")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionById(@PathVariable("id") Long id) {
        return questionService.queryQuestionById(id);
    }
    
    /**
     * 删除我的问题
     */
    @Operation(summary ="删除我的问题")
    @DeleteMapping("/{id}")
    public void removeMyQuestion(@PathVariable("id") Long id) {
        questionService.removeMyQuestion(id);
    }

}
