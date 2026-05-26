package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.enums.QuestionStatus;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {
    
    private final IInteractionQuestionService questionService;
    
    private final UserClient userClient;
    
    private final RemarkClient remarkClient;
    
    private final StringRedisTemplate redisTemplate;
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 新增回答或评论
     */
    @Override
    @Transactional
    public void addReply(ReplyDTO replyDTO) {
        Long userId = UserContext.getUser();
        // 1. 写入回答或评论
        InteractionReply reply = BeanUtil.toBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        save(reply);
        // 2. 判断是否是回答
        if (reply.getAnswerId() == null) {
            // 修改最近一次回答ID、累加回答次数
            questionService.lambdaUpdate()
                    .set(InteractionQuestion::getLatestAnswerId, reply.getId())
                    .setSql("answer_times = answer_times + 1")
                    .eq(InteractionQuestion::getId, reply.getQuestionId())
                    .update();
        } else {
            // 累加评论次数
            lambdaUpdate()
                    .setSql("reply_times = reply_times + 1")
                    .eq(InteractionReply::getId, reply.getTargetReplyId())
                    .update();
        }
        // 3. 判断是否是学生提交（null 也是学生）
        if (!BooleanUtil.isFalse(replyDTO.getIsStudent())) {
            // 标记问题状态为未查看
            questionService.lambdaUpdate()
                    .set(InteractionQuestion::getStatus, QuestionStatus.UN_CHECK)
                    .eq(InteractionQuestion::getId, reply.getQuestionId())
                    .update();
        }
        // 4. 添加问答积分
        rabbitTemplate.convertAndSend(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.WRITE_REPLY,
                userId
        );
    }
    
    /**
     * 分页查询回答或评论列表
     */
    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {
        Long answerId = query.getAnswerId();
        Long questionId = query.getQuestionId();
        if (answerId == null && questionId == null) {
            throw new BadRequestException("问题与回答至少指定一个");
        }
        /*
            1. 分页查询回答或评论列表
            查询回答列表, 需要附带 answer_id = 0
            因为添加评论也会绑定问题ID, 添加回答会将answer_id设置为0
         */
        answerId = answerId == null ? 0L : answerId;
        Page<InteractionReply> page = lambdaQuery()
                .eq(InteractionReply::getAnswerId, answerId)
                .eq(questionId != null, InteractionReply::getQuestionId, questionId)
                .eq(InteractionReply::getHidden, false)
                .page(query.toMpPage("liked_times", false)); // 根据点赞数量降序
        List<InteractionReply> replyList = page.getRecords();
        if (CollUtils.isEmpty(replyList)) {
            return PageDTO.empty(page);
        }
        // 2. 获取用户ID列表和回答ID列表
        Set<Long> userIds = new HashSet<>();
        List<Long> replyIds = new ArrayList<>(replyList.size());
        for (InteractionReply reply : replyList) {
            if (!reply.getAnonymity()) {
                userIds.add(reply.getUserId());
            }
            if (answerId != 0) { // user_name 回复 user_name
                userIds.add(reply.getTargetUserId());
            }
            replyIds.add(reply.getId());
        }
        // 3. 查询用户信息
        Map<Long, UserDTO> userMap = queryUserDTOMap(userIds);
        // 4. 查询点赞列表 (提交replyIds, 将用户点赞的reply返回)
        Set<Long> ids = remarkClient.queryLikedStatus(replyIds);
        // 5. 查询点赞数量 (如果Redis有, 会覆盖Mysql的点赞数量)
        Object[] array = replyIds.stream().map(Object::toString).toArray();
        List<Double> scores = redisTemplate.opsForZSet().score("likes:times:type:QA", array);
        // 6. 封装VOList
        List<ReplyVO> voList = new ArrayList<>(replyList.size());
        for (int i = 0; i < replyList.size(); i++) {
            InteractionReply reply = replyList.get(i);
            ReplyVO vo = BeanUtil.toBean(reply, ReplyVO.class);
            voList.add(vo);
            // 6-1. 设置用户信息和目标用户名字
            vo.setUserId(null);
            UserDTO user = userMap.get(reply.getUserId());
            if (!reply.getAnonymity() && user != null) {
                vo.setUserId(user.getId());
                vo.setUserName(user.getName());
                vo.setUserIcon(user.getIcon());
            }
            if (answerId != 0) { // 是评论
                user = userMap.get(reply.getTargetUserId());
                vo.setTargetUserName(user.getName());
            }
            // 6-2. 设置用户是否点赞
            vo.setLiked(ids.contains(reply.getId()));
            // 6-3. 设置点赞数量
            Double score = scores.get(i);
            if (score != null) {
                vo.setLikedTimes(score.intValue());
            }
        }
        return PageDTO.of(page, voList);
    }
    
    /**
     * 管理端分页查询回答或评论列表
     */
    @Override
    public PageDTO<ReplyVO> queryAdminReplyPage(ReplyPageQuery query) {
        Long answerId = query.getAnswerId();
        Long questionId = query.getQuestionId();
        if (questionId == null && answerId == null) {
            throw new BadRequestException("问题与回答至少指定一个");
        }
        /*
            1. 分页查询回答或评论列表
            查询回答列表, 需要附带 answer_id = 0
            因为添加评论也会绑定问题ID, 添加回答会将answer_id设置为0
         */
        answerId = answerId == null ? 0L : answerId;
        Page<InteractionReply> page = lambdaQuery()
                .eq(InteractionReply::getAnswerId, answerId)
                .eq(questionId != null, InteractionReply::getQuestionId, questionId)
                .page(query.toMpPage("liked_times", false)); // 根据点赞数量降序
        List<InteractionReply> replyList = page.getRecords();
        if (CollUtils.isEmpty(replyList)) {
            return PageDTO.empty(page);
        }
        // 2. 获取用户ID和回答ID
        Set<Long> userIds = new HashSet<>();
        List<Long> replyIds = new ArrayList<>(replyList.size());
        for (InteractionReply reply : replyList) {
            userIds.add(reply.getUserId());
            replyIds.add(reply.getId());
        }
        // 3. 查询用户信息
        Map<Long, UserDTO> userMap = queryUserDTOMap(userIds);
        // 4. 查询点赞列表 (提交replyIds, 将用户点赞的reply返回)
        Set<Long> ids = remarkClient.queryLikedStatus(replyIds);
        // 5. 查询点赞数量 (如果Redis有, 会覆盖Mysql的点赞数量)
        Object[] array = replyIds.stream().map(Object::toString).toArray();
        List<Double> scores = redisTemplate.opsForZSet().score("likes:times:type:QA", array);
        // 6. 封装VOList
        List<ReplyVO> voList = new ArrayList<>(replyList.size());
        for (int i = 0; i < replyList.size(); i++) {
            InteractionReply reply = replyList.get(i);
            ReplyVO vo = BeanUtil.toBean(reply, ReplyVO.class);
            voList.add(vo);
            // 6.1. 设置用户信息和目标用户名字
            vo.setUserId(null);
            UserDTO user = userMap.get(reply.getUserId());
            if (user != null) {
                vo.setUserId(user.getId());
                vo.setUserName(user.getName());
                vo.setUserIcon(user.getIcon());
            }
            if (answerId != 0) { // 是评论
                user = userMap.get(reply.getTargetUserId());
                vo.setTargetUserName(user.getName());
            }
            // 6.2. 设置用户是否点赞
            vo.setLiked(ids.contains(reply.getId()));
            // 6.3. 设置点赞数量
            Double score = scores.get(i);
            if (score != null) {
                vo.setLikedTimes(score.intValue());
            }
        }
        return PageDTO.of(page, voList);
    }
    
    /**
     * 管理端隐藏或显示评论
     */
    @Override
    public void showOrHiddenReply(Long id, boolean hidden) {
        lambdaUpdate()
                .set(InteractionReply::getHidden, hidden)
                .eq(InteractionReply::getId, id)
                .update();
    }
    
    @Override
    public ReplyVO queryAdminReplyById(Long id) {
        // 1. 查询所需信息
        // 1.1. 查询回答信息
        InteractionReply reply = getById(id);
        // 1.2. 查询用户信息
        UserDTO user = userClient.queryUserById(reply.getUserId());
        // 1.3. 查询点赞信息
        Set<Long> ids = remarkClient.queryLikedStatus(List.of(id));
        // 1.4. 查询点赞数量
        Double score = redisTemplate.opsForZSet().score("likes:times:type:QA", id.toString());
        // 2. 封装VO
        // 2.1. 将回答信息拷贝的VO
        ReplyVO vo = BeanUtil.copyProperties(reply, ReplyVO.class);
        // 2.2. 设置用户信息
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        // 2.3. 设置用户是否点赞
        vo.setLiked(ids.contains(id));
        // 2.4. 设置点赞数量
        if (score != null) {
            vo.setLikedTimes(score.intValue());
        }
        return vo;
    }
    
    /**
     * 查询用户信息
     */
    private Map<Long, UserDTO> queryUserDTOMap(Iterable<Long> userIds) {
        List<UserDTO> userList = userClient.queryUserByIds(userIds);
        return userList.stream()
                .collect(Collectors.toMap(UserDTO::getId, u -> u));
    }
}
