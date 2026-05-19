package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-17
 * @Description: 课表管理Service接口
 */
public interface ILearningLessonService extends IService<LearningLesson> {
    
    /**
     * 分页查询我的课表; 根据latest_learn_time（最后一次学习时间）或create_time（购买时间排序）
     */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);
    
    /**
     * 批量添加用户课程
     */
    void addUserLessons(Long userId, List<Long> courseIds);
    
    /**
     * 查询正在学习的课程
     */
    LearningLessonVO queryMyCurrentLesson();
    
    /**
     * 删除课程
     */
    void removeLearningLesson(Long userId, List<Long> courseIds);
    
    /**
     * 校验课程是否有效
     */
    Long checkCourseValid(Long courseId);
    
    /**
     * 查询用户课表中指定课程状态
     */
    LearningLessonVO queryCourseStatus(Long courseId);
    
    /**
     * 统计课程学习人数
     */
    Long countLearningLessonByCourse(Long courseId);
    
    /**
     * 创建学习计划
     */
    void addLearningPlan(LearningPlanDTO learningPlanDTO);
    
    /**
     * 查询我的学习计划
     */
    LearningPlanPageVO queryMyPlan(PageQuery query);
}
