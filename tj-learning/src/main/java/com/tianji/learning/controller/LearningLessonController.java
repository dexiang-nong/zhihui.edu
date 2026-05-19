package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-17
 * @Description: 课表管理
 */
@RestController
@Tag(name = "课表查询管理相关接口")
@RequiredArgsConstructor
@RequestMapping("/lessons")
public class LearningLessonController {

    private final ILearningLessonService learningLessonService;
    
    /**
     * 分页查询我的课表; 根据latest_learn_time（最后一次学习时间）或create_time（购买时间排序）
     */
    @Operation(summary = "分页查询我的课表; 根据latest_learn_time（最后一次学习时间）或create_time（购买时间排序）")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        return learningLessonService.queryMyLessons(query);
    }
    
    /**
     * 查询正在学习的课程
     */
    @Operation(summary = "查询正在学习的课程")
    @GetMapping("/now")
    public LearningLessonVO queryMyCurrentLesson() {
        return learningLessonService.queryMyCurrentLesson();
    }
    
    /**
     * 删除课程
     */
    @Operation(summary = "删除课程")
    @DeleteMapping("/{courseId}")
    public void removeLearningLesson(@PathVariable Long courseId) {
        learningLessonService.removeLearningLesson(UserContext.getUser(), List.of(courseId));
    }
    
    /**
     * 校验课程是否有效
     */
    @Operation(summary = "校验课程是否有效")
    @GetMapping("/{courseId}/valid")
    public Long checkCourseValid(@PathVariable Long courseId) {
        return learningLessonService.checkCourseValid(courseId);
    }
    
    /**
     * 查询用户指定课程状态
     */
    @Operation(summary = "查询用户课表中指定课程状态")
    @GetMapping("/{courseId}")
    public LearningLessonVO queryCourseStatus(@PathVariable Long courseId) {
        return learningLessonService.queryCourseStatus(courseId);
    }
    
    /**
     * 统计课程学习人数
     */
    @Operation(summary = "统计课程学习人数")
    @GetMapping("/{courseId}/count")
    public Long countLearningLessonByCourse(@PathVariable Long courseId) {
        return learningLessonService.countLearningLessonByCourse(courseId);
    }
    
    /**
     * 创建学习计划
     */
    @Operation(summary = "创建学习计划")
    @PostMapping("/plans")
    public void addLearningPlan(@Valid @RequestBody LearningPlanDTO learningPlanDTO) {
        learningLessonService.addLearningPlan(learningPlanDTO);
    }
    
    /**
     * 查询我的学习计划
     */
    @Operation(summary = "查询我的学习计划")
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlan(PageQuery query) {
        return learningLessonService.queryMyPlan(query);
    }
}
