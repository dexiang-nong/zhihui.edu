package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.domain.po.ChatSession;
import com.tianji.aigc.domain.vo.MessageVO;
import com.tianji.aigc.domain.vo.SessionVO;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.IChatSessionService;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
        String conversationId = ChatService.getConversationId(sessionId);
        
        List<Message> messageList = chatMemory.get(conversationId);
        return messageList.stream()
                // 过滤掉非用户消息和助手消息
                .filter(message -> MessageType.ASSISTANT == message.getMessageType()  || MessageType.USER == message.getMessageType())
                // 转换为MessageVO对象
                .map(message -> MessageVO.builder()
                        .content(message.getText())
                        .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, List<SessionVO>> queryHistorySession() {
        // 查询用户所有会话列表
        Long userId = UserContext.getUser();
        List<ChatSession> list = lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime)
                .list();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStartTime = DateUtils.getDayStartTime(now); // 今天
        LocalDateTime thirtyDaysAgo = dayStartTime.minusDays(30);    // 30天前
        LocalDateTime oneYearAgo = dayStartTime.minusYears(1);       // 1年前
        
        Map<String, List<SessionVO>> result = new HashMap<>();
        result.put("当天", new ArrayList<>());
        result.put("最近30天", new ArrayList<>());
        result.put("最近1年", new ArrayList<>());
        result.put("1年以上", new ArrayList<>());
        
        for (ChatSession session : list) {
            SessionVO sessionVO = SessionVO.builder()
                    .sessionId(session.getSessionId())
                    .title(session.getTitle())
                    .updateTime(session.getUpdateTime())
                    .build();
            
            LocalDateTime updateTime = session.getUpdateTime();
            if (updateTime.isAfter(dayStartTime)) {
                result.get("当天").add(sessionVO);
            } else if (updateTime.isAfter(thirtyDaysAgo)) {
                result.get("最近30天").add(sessionVO);
            } else if (updateTime.isAfter(oneYearAgo)) {
                result.get("最近1年").add(sessionVO);
            } else {
                result.get("1年以上").add(sessionVO);
            }
        }
        
        return result;
    }
    
    @Override
    public void putHistorySessionTitle(String sessionId, String title) {
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
}
