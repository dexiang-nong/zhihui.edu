package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.domain.po.ChatSession;
import com.tianji.aigc.domain.vo.SessionVO;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.service.IChatSessionService;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
