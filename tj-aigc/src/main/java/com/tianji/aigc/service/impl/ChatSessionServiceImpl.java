package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.domain.po.ChatSession;
import com.tianji.aigc.domain.vo.MessageVO;
import com.tianji.aigc.domain.vo.SessionVO;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.memory.MyAssistantMessage;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.IChatSessionService;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 对话session 服务实现类
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-22
 */
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements IChatSessionService {
    
    private final SessionProperties sessionProperties;
    
    @Override
    public SessionVO createSession(Integer num) {
        // 拷贝属性 (标题、描述)
        SessionVO sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机选取几个例子
        sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(), num));
        
        // 生成会话ID
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());
        
        // 保存会话记录
        ChatSession chatSession = ChatSession.builder()
                .sessionId(sessionVO.getSessionId())
                .userId(UserContext.getUser())
                .build();
        save(chatSession);
        
        return sessionVO;
    }
    
    @Override
    public List<SessionVO.Example> hotExamples(Integer num) {
        return RandomUtil.randomEleList(sessionProperties.getExamples(), num);
    }
    
    private final ChatMemory chatMemory;
    
    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        // 拼接对话id: userId_sessionId
        String conversationId = ChatService.getConversationId(sessionId);
        
        List<Message> messageList = chatMemory.get(conversationId);
        return messageList.stream()
                // 过滤掉非用户消息和助手消息
                .filter(message -> MessageType.ASSISTANT == message.getMessageType()  || MessageType.USER == message.getMessageType())
                // 转换为MessageVO对象
                .map(message -> {
                    // 有params参数需要返回 (课程卡片信息)
                    if (message instanceof MyAssistantMessage myAssistantMessage) {
                        return MessageVO.builder()
                                .content(message.getText())
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .params(myAssistantMessage.getParams())
                                .build();
                    }
                    return MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, List<SessionVO>> queryHistorySession() {
        // 查询用户会话列表
        Long userId = UserContext.getUser();
        List<ChatSession> list = lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .isNotNull(ChatSession::getTitle)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT 30")
                .list();
        if (CollUtils.isEmpty(list)) {
            return Map.of();
        }
        // 转换为响应VO
        List<SessionVO> voList = list.stream()
                .map(session -> SessionVO.builder()
                        .sessionId(session.getSessionId())
                        .title(session.getTitle())
                        .updateTime(session.getUpdateTime())
                        .build())
                .toList();
        
        // 根据时间分组
        final var TODAY = "当天";
        final var LAST_30_DAYS = "最近30天";
        final var LAST_YEAR = "最近1年";
        final var MORE_THAN_YEAR = "1年以上";
        LocalDate today = LocalDateTime.now().toLocalDate();
        return voList.stream()
                .collect(Collectors.groupingBy(session -> {
                    // 计算两个日期的天数差
                    long day = ChronoUnit.DAYS.between(session.getUpdateTime().toLocalDate(), today);
                    if (day == 0) return TODAY;
                    if (day <= 30) return LAST_30_DAYS;
                    if (day <= 365) return LAST_YEAR;
                    return MORE_THAN_YEAR;
                }));
    }
    
    @Override
    public void updateHistorySessionTitle(String sessionId, String title) {
        title = StrUtil.sub(title, 0, 100);
        lambdaUpdate()
                .set(ChatSession::getTitle, title)
                .eq(ChatSession::getSessionId, sessionId)
                .update();
    }
    
    @Override
    public void deleteHistorySession(String sessionId) {
        // 删除会话历史
        lambdaUpdate()
                .eq(ChatSession::getSessionId, sessionId)
                .remove();
        // 删除会话记录
        chatMemory.clear(ChatService.getConversationId(sessionId));
    }
    
    @Async // 异步更新，不影响AI响应
    @Override
    public void asyncUpdateHistorySessionTitle(String sessionId, String title) {
        ChatSession session = lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .one();
        if (session == null) {
            return;
        }
        if (StrUtil.isBlank(session.getTitle()) && StrUtil.isNotBlank(title)) {
            session.setTitle(title);
        }
        session.setUpdateTime(LocalDateTime.now());
        updateById(session);
    }
}
