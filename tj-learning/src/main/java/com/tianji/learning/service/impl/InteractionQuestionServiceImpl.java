package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.enums.QuestionStatus;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {
    
    private final InteractionReplyMapper replyMapper;
    
    private final UserClient userClient;
    
    private final SearchClient searchClient;
    
    private final CourseClient courseClient;
    
    private final CatalogueClient catalogueClient;
    
    private final CategoryCache categoryCache;
    
    /**
     * 新增互动问题
     */
    @Override
    public InteractionQuestion addQuestion(QuestionFormDTO questionFormDTO) {
        // 1. 转换实体
        InteractionQuestion entity = BeanUtil.toBean(questionFormDTO, InteractionQuestion.class);
        // 2. 封装其他属性
        entity.setUserId(UserContext.getUser());
        // 3. 写道数据库
        save(entity);
        
        return entity;
    }
    
    /**
     * 修改互动问题
     */
    @Override
    public void updateQuestion(Long id, QuestionFormDTO questionFormDTO) {
        // 1. 转换实体
        InteractionQuestion entity = BeanUtil.toBean(questionFormDTO, InteractionQuestion.class);
        // 2. 封装其他属性
        entity.setId(id);
        // 3. 写入数据库
        updateById(entity);
    }
    
    /**
     * 用户端分页查询问题
     */
    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }
        // 分页查询问题列表
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description")) // 排除问题描述信息
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser()) // 查询我的问题
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> questionList = page.getRecords();
        if (CollUtils.isEmpty(questionList)) {
            return PageDTO.empty(page);
        }
        // 查询用户信息和最新回答信息
        // 1. 获取用户ID列表和最新回答ID列表
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();
        questionList.forEach(question -> {
            if (!question.getAnonymity()) {
                userIds.add(question.getUserId());
            }
            answerIds.add(question.getLatestAnswerId());
        });
        // 2. 查询最新回答信息列表
        answerIds.remove(null);
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if (CollUtils.isNotEmpty(answerIds)) {
            List<InteractionReply> replyList = replyMapper.selectBatchIds(answerIds);
            replyList.forEach(reply -> {
                replyMap.put(reply.getId(), reply);
                if (!reply.getAnonymity()) { // 匿名用户不做查询
                    userIds.add(reply.getUserId());
                }
            });
        }
        // 3. 查询用户信息列表
        userIds.remove(null);
        Map<Long, UserDTO> userMap = queryUserMap(userIds);
        // 4. 封装用户信息和最新回答信息
        List<QuestionVO> voList = new ArrayList<>(questionList.size());
        for (InteractionQuestion question : questionList) {
            // 4.1. 将问题信息拷贝到VO
            QuestionVO vo = BeanUtil.toBean(question, QuestionVO.class);
            voList.add(vo);
            vo.setUserId(null);
            // 3.2. 设置提问者信息
            if (!question.getAnonymity()) {
                UserDTO user = userMap.get(question.getUserId());
                if (user != null) {
                    vo.setUserId(user.getId());
                    vo.setUserName(user.getName());
                    vo.setUserIcon(user.getIcon());
                }
            }
            // 4.3. 设置最新回答信息
            InteractionReply reply = replyMap.get(question.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                if (!reply.getAnonymity()) { // 忽略匿名用户
                    vo.setLatestReplyUser(userMap.get(reply.getUserId()).getName());
                }
            }
        }
        return PageDTO.of(page, voList);
    }
    
    /**
     * 根据id查询问题详情
     */
    @Override
    public QuestionVO queryQuestionById(Long id) {
        // 1. 查询问题信息
        InteractionQuestion question = getById(id);
        if (question == null || question.getHidden()) {
            return null;
        }
        QuestionVO vo = BeanUtil.toBean(question, QuestionVO.class);
        // 2. 查询提问者信息
        if (!question.getAnonymity()) {
            UserDTO user = userClient.queryUserById(question.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
                vo.setUserIcon(user.getIcon());
            }
        }
        return vo;
    }
    
    /**
     * 删除我的问题
     */
    @Override
    @Transactional
    public void removeMyQuestion(Long id) {
        // 1. 查询问题
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题已经被删除");
        }
        // 2. 是否有当前用户提问
        if (!UserContext.getUser().equals(question.getUserId())) {
            throw new BadRequestException("你不能删除他人的提问");
        }
        // 3. 删除问题
        removeById(id);
        // 4. 删除问题关联的回答
        replyMapper.delete(
                Wrappers.lambdaQuery(InteractionReply.class)
                        .eq(InteractionReply::getQuestionId, id)
        );
    }
    
    /**
     * 管理端分页查询问题
     */
    @Override
    public PageDTO<QuestionAdminVO> queryAdminQuestionPage(QuestionAdminPageQuery query) {
        // 1. 查询课程 (Elasticsearch)
        List<Long> courseIdList = null;
        String courseName = query.getCourseName();
        if (StrUtil.isNotBlank(courseName)) {
            courseIdList = searchClient.queryCoursesIdByName(courseName);
            if (CollUtils.isEmpty(courseIdList)) {
                return PageDTO.empty(0L, 0L);
            }
        }
        // 2. 分页查询问题列表
        Integer status = query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIdList != null, InteractionQuestion::getCourseId, courseIdList)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .ge(begin != null, InteractionQuestion::getCreateTime, begin)
                .le(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> questionList = page.getRecords();
        if (CollUtils.isEmpty(questionList)) {
            return PageDTO.empty(page);
        }
        // 3. 查询用户信息、课程信息、章节信息
        // 获取用户ID列表、课程ID列表、章节ID列表
        Set<Long> userIds = new HashSet<>();
        Set<Long> courseIds = new HashSet<>();
        Set<Long> catalogueIds = new HashSet<>();
        questionList.forEach(question -> {
            userIds.add(question.getUserId());
            courseIds.add(question.getCourseId());
            catalogueIds.add(question.getChapterId()); // 章ID
            catalogueIds.add(question.getSectionId()); // 节ID
        });
        // 3.1. 查询用户列表
        Map<Long, UserDTO> userMap = queryUserMap(userIds);
        // 3.2. 查询课程列表
        List<CourseSimpleInfoDTO> courseList = courseClient.getSimpleInfoList(courseIds);
        Map<Long, CourseSimpleInfoDTO> courseMap = new HashMap<>();
        if (CollUtils.isNotEmpty(courseList)) {
            courseMap = courseList.stream()
                    .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }
        // 3.3. 查询章节列表
        Map<Long, String> catalogueMap = queryCatalogueMap(catalogueIds);
        // 4. 封装用户信息、课程信息、章节信息
        List<QuestionAdminVO> voList = new ArrayList<>(questionList.size());
        for (InteractionQuestion question : questionList) {
            // 4.1. 将问题信息拷贝到VO
            QuestionAdminVO vo = BeanUtil.toBean(question, QuestionAdminVO.class);
            voList.add(vo);
            // 4.2. 设置用户信息
            UserDTO user = userMap.get(question.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
            }
            // 4.3. 设置课程信息、分类信息
            CourseSimpleInfoDTO course = courseMap.get(question.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getName());
                vo.setCategoryName(categoryCache.getCategoryNames(course.getCategoryIds()));
            }
            // 4.4. 设置章名称、节名称
            vo.setChapterName(catalogueMap.getOrDefault(question.getChapterId(), ""));
            vo.setSectionName(catalogueMap.getOrDefault(question.getSectionId(), ""));
        }
        return PageDTO.of(page, voList);
    }
    
    /**
     * 管理端隐藏或显示问题
     */
    @Override
    public void showOrHiddenQuestion(Long id, boolean hidden) {
        lambdaUpdate()
                .set(InteractionQuestion::getHidden, hidden)
                .eq(InteractionQuestion::getId, id)
                .update();
    }
    
    /**
     * 管理端根据id查询问题详情
     */
    @Override
    public QuestionAdminVO queryAdminQuestionById(Long id) {
        // 1. 查询所需信息
        // 1.1. 查询问题信息
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题已经被删除");
        }
        // 1.1. 查询用户信息
        UserDTO user = userClient.queryUserById(question.getUserId());
        // 1.2. 查询课程信息
        CourseFullInfoDTO course = courseClient.getCourseInfoById(question.getCourseId(), false, true);
        // 1.3. 查询章节信息
        Map<Long, String> catalogueMap = queryCatalogueMap(Set.of(question.getChapterId(), question.getSectionId()));
        // 2. 封装VO
        // 2.1. 将问题信息拷贝到VO
        QuestionAdminVO vo = BeanUtil.toBean(question, QuestionAdminVO.class);
        // 2.2. 设置用户信息
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        // 2.2. 设置课程信息、分类信息
        if (course != null) {
            vo.setCourseName(course.getName());
            vo.setCategoryName(categoryCache.getCategoryNames(course.getCategoryIds()));
        }
        // 2.3. 设置章名称、节名称
        vo.setChapterName(catalogueMap.getOrDefault(question.getChapterId(), ""));
        vo.setSectionName(catalogueMap.getOrDefault(question.getSectionId(), ""));
        // 2.4. 设置负责老师
        if (course != null) {
            List<UserDTO> userList = userClient.queryUserByIds(course.getTeacherIds());
            String teacherNames = userList.stream()
                    .map(UserDTO::getName)
                    .collect(Collectors.joining("/"));
            vo.setTeacherName(teacherNames);
        }
        // 3. 标记已查看
        lambdaUpdate()
                .set(InteractionQuestion::getStatus, QuestionStatus.CHECKED)
                .eq(InteractionQuestion::getId, id)
                .update();
        return vo;
    }
    
    /**
     * 查询用户信息
     */
    private Map<Long, UserDTO> queryUserMap(Iterable<Long> userIds) {
        List<UserDTO> userList = userClient.queryUserByIds(userIds);
        if (CollUtils.isNotEmpty(userList)) {
            return userList.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        return new HashMap<>();
    }
    
    /**
     * 查询章节列表
     */
    private Map<Long, String> queryCatalogueMap(Iterable<Long> ids) {
        List<CataSimpleInfoDTO> catalogueList = catalogueClient.batchQueryCatalogue(ids);
        if (CollUtils.isNotEmpty(catalogueList)) {
            return catalogueList.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        return new HashMap<>();
    }
    
}
