package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.enums.LessonStatus;
import com.tianji.learning.domain.enums.PlanStatus;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-17
 * @Description: 课表管理Service实现
 */
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {
    
    private final CourseClient courseClient;
    
    private final CatalogueClient catalogueClient;
    
    private final LearningRecordMapper recordMapper;
    
    /**
     * 分页查询我的课表; 根据latest_learn_time（最后一次学习时间）或create_time（购买时间排序）
     */
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        // 1. 获取当前用户ID
        Long userId = UserContext.getUser();
        // 2. 分页查询出用户学习的所有课程
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> LessonList = page.getRecords();
        if (CollUtils.isEmpty(LessonList)) {
            return PageDTO.empty(page);
        }
        // 3. 获取学习课程详细信息
        // 3-1. 获取所有课程ID
        Set<Long> courseIds = LessonList.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        // 3-2. 查询课程信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(courseIds);
        // 4. 封装VO
        List<LearningLessonVO> voList = new ArrayList<>(cMap.size());
        LessonList.forEach(learningLesson -> {
            // 4-1. 实体转VO
            LearningLessonVO learningLessonVO = BeanUtil.toBean(learningLesson, LearningLessonVO.class);
            // 4-2. 获取课程信息，填充到VO
            CourseSimpleInfoDTO courseSimpleInfoDTO = cMap.get(learningLesson.getCourseId());
            learningLessonVO.setCourseName(courseSimpleInfoDTO.getName());
            learningLessonVO.setCourseCoverUrl(courseSimpleInfoDTO.getCoverUrl());
            learningLessonVO.setSections(courseSimpleInfoDTO.getSectionNum());
            
            voList.add(learningLessonVO);
        });
        
        return PageDTO.of(page, voList);
    }
    
    /**
     * 批量添加用户课程
     */
    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        // 1. 获取学习课程详细信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(courseIds);
        // 2. 组装学习课程信息
        List<LearningLesson> LessonList = new ArrayList<>(courseIds.size());
        cMap.forEach((courseId, cDTO) -> {
            LearningLesson Lesson = new LearningLesson();
            // 2-1. 填充用户ID和课程ID
            Lesson.setUserId(userId);
            Lesson.setCourseId(courseId);
            // 2-2. 填充创建时间和结束时间
            Integer validDuration = cDTO.getValidDuration();
            if (validDuration != null && validDuration > 0) {
                LocalDateTime now = LocalDateTime.now();
                Lesson.setCreateTime(now);
                Lesson.setExpireTime(now.plusMonths(validDuration));
            }
            LessonList.add(Lesson);
        });
        // 3. 批量保存
        saveBatch(LessonList);
    }
    
    /**
     * 批量删除课程
     */
    @Override
    public void removeLearningLesson(Long userId, List<Long> courseIds) {
        lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds)
                .remove();
    }
    
    /**
     * 查询正在学习的课程
     */
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        // 1. 获取当前用户ID
        Long userId = UserContext.getUser();
        // 2. 分页查询出用户学习的所有课程
        LearningLesson Lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (Lesson == null) return null;
        
        // 3. 转换VO
        LearningLessonVO vo = BeanUtil.toBean(Lesson, LearningLessonVO.class);
        // 4. 查询课程信息
        CourseFullInfoDTO cInfo = courseClient
                .getCourseInfoById(vo.getCourseId(), false, false);
        vo.setCourseName(cInfo.getName());
        vo.setSections(cInfo.getSectionNum());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        // 5. 统计课表中的课程数量
        Long courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        vo.setCourseAmount(courseAmount.intValue());
        // 6. 查询小节信息
        List<CataSimpleInfoDTO> cataInfos = catalogueClient
                .batchQueryCatalogue(CollUtils.singletonList(Lesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataInfos)) {
            CataSimpleInfoDTO cataInfo = cataInfos.get(0);
            vo.setLatestSectionName(cataInfo.getName());
            vo.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return vo;
    }
    
    /**
     * 校验课程是否有效
     */
    @Override
    public Long checkCourseValid(Long courseId) {
        // 1. 该课程在用户中是否过期
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, UserContext.getUser())
                .eq(LearningLesson::getCourseId, courseId)
                .ne(LearningLesson::getStatus, LessonStatus.EXPIRED)
                .one();
        // 2. 返回课程ID
        return lesson == null ? null : lesson.getId();
    }
    
    /**
     * 查询用户课表中指定课程状态
     */
    @Override
    public LearningLessonVO queryCourseStatus(Long courseId) {
        // 1. 查询用户是否有该课程
        LearningLesson learningLesson = lambdaQuery()
                .eq(LearningLesson::getUserId, UserContext.getUser())
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (learningLesson == null) {
            return null;
        }
        // 2. 返回课程的学习进度、课程有效期等信息。
        return BeanUtil.toBean(learningLesson, LearningLessonVO.class);
    }
    
    /**
     * 统计课程学习人数
     */
    @Override
    public Long countLearningLessonByCourse(Long courseId) {
        return lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .ne(LearningLesson::getStatus, LessonStatus.NOT_BEGIN) // 排除未学习状态的
                .count();
    }
    
    /**
     * 创建学习计划
     */
    @Override
    public void addLearningPlan(LearningPlanDTO learningPlanDTO) {
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, UserContext.getUser())
                .eq(LearningLesson::getCourseId, learningPlanDTO.getCourseId())
                .one();
        if (lesson == null) {
            throw new BadRequestException("课程信息不存在");
        }
        LearningLesson lessonUpdate = new LearningLesson();
        lessonUpdate.setId(lesson.getId());
        lessonUpdate.setWeekFreq(learningPlanDTO.getFreq());
        if (lesson.getPlanStatus() ==  PlanStatus.NO_PLAN) {
            lessonUpdate.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        updateById(lessonUpdate);
    }
    
    /**
     * 查询我的学习计划
     */
    @Override
    public LearningPlanPageVO queryMyPlan(PageQuery query) {
        LearningPlanPageVO result = new LearningPlanPageVO();
        // 1. 查询总的统计数据 (封装LearningPlanPageVO的3个属性值)
        Long userId = UserContext.getUser();
        LocalDate now = LocalDate.now();
        LocalDateTime begin = DateUtils.getWeekBeginTime(now);
        LocalDateTime end = DateUtils.getWeekEndTime(now);
        // 1.1. 本周总的已学习小节数量
        Long weekFinished = recordMapper.selectCount(
                Wrappers.lambdaQuery(LearningRecord.class)
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getFinished, true)
                        .gt(LearningRecord::getFinishTime, begin)
                        .lt(LearningRecord::getFinishTime, end)
        );
        result.setWeekFinished(weekFinished.intValue());
        // 1.2. 本周的总计划学习小节数量
        Integer weekTotalPlan = recordMapper.queryTotalPlan(userId);
        result.setWeekTotalPlan(weekTotalPlan);
        // 1.3. 本周总的学习积分 TODO
        
        // 2. 查询课表信息以及学习计划信息 (封装list:LearningPlanVO数据)
        // 2.1. 分页查询有计划的课表信息
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return result.pageInfo(PageDTO.empty(page));
        }
        // 2.2. 查询课表对应的课程信息
        Set<Long> courseIds = records.stream()
                .map(LearningLesson::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(courseIds);
        // 2.3. 统计每一个课程本周已学习小节数量
        List<IdAndNumDTO> countList = recordMapper.countLearnedSections(userId, begin, end);
        Map<Long, Integer> countMap = IdAndNumDTO.toMap(countList);
        // 3. 封装voList
        List<LearningPlanVO> voList = new ArrayList<>(records.size());
        for (LearningLesson record : records) {
            // 3.1. 拷贝基础属性到vo
            LearningPlanVO vo = BeanUtils.copyBean(record, LearningPlanVO.class);
            voList.add(vo);
            // 3.2. 填充课程详细信息
            CourseSimpleInfoDTO cInfo = cMap.get(record.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setSections(cInfo.getSectionNum());
            }
            // 3.3. 每个课程的本周已学习小节数量
            vo.setWeekLearnedSections(countMap.getOrDefault(record.getId(), 0));
        }
        return result.pageInfo(PageDTO.of(page, voList));
    }
    
    /**
     * 获取所有课程详情，并组装到Map中
     */
    private Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(Iterable<Long> courseIds) {
        // 1. 查询所有课程详情
        List<CourseSimpleInfoDTO> cDTOList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cDTOList)) {
            throw new BadRequestException("课程信息不存在");
        }
        // 2. 组装课程详情
        return cDTOList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
    }
}
