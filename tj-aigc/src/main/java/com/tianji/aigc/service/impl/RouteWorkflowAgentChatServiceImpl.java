package com.tianji.aigc.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tianji.aigc.agent.AbstractAgent;
import com.tianji.aigc.agent.Agent;
import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.IChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * <p>
 * 路由工作流智能体：多智能体协调工作
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-25
 */
@Service
@RequiredArgsConstructor
// 该服务存在两个智能体方案：【增强型智能体：ENHANCE】【路由工作流智能体：ROUTE】，只能使用一个，所以添加条件
@ConditionalOnProperty(prefix = "tj.ai", value = "chat-type", havingValue = "ROUTE")
public class RouteWorkflowAgentChatServiceImpl implements ChatService {
    
    private final IChatSessionService chatSessionService;
    
    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // 更新会话标题、会话时间
        String title = StrUtil.sub(question, 0, 100);
        chatSessionService.asyncUpdateHistorySessionTitle(sessionId, title);
        
        // 先通过路由智能体，分析用户的意图，再执行后面的逻辑
        String result = findAgentByType(AgentTypeEnum.ROUTE).process(question, sessionId);
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.of(result);
        
        Agent agent = findAgentByType(agentTypeEnum);
        if (agent == null) {
            // 找不到对应的智能体，直接返回结果
            ChatEventVO chatEventVO = ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(result)
                    .build();
            return Flux.just(chatEventVO, AbstractAgent.STOP_EVENT);
        }
        // 执行智能体的逻辑
        return agent.processStream(question, sessionId);
    }
    
    /**
     * 根据代理类型查找对应的Agent实例
     *
     * @param agentTypeEnum 要查找的代理类型
     * @return 与给定类型匹配的Agent实例，如果未找到或类型为null则返回null
     */
    private Agent findAgentByType(AgentTypeEnum agentTypeEnum) {
        if (agentTypeEnum == null) {
            return null;
        }
        // 从IOC容器中获取所有Agent Bean<beanName, bean>
        Map<String, Agent> beans = SpringUtil.getBeansOfType(Agent.class);
        for (Agent agent : beans.values()) {
            if (agentTypeEnum == agent.getAgentType()) {
                return agent;
            }
        }
        return null;
    }
    
    /**
     * 停止生成
     *
     * @param sessionId 会话ID
     */
    @Override
    public void stop(String sessionId) {
        findAgentByType(AgentTypeEnum.ROUTE).stop(sessionId);
    }
    
    private final ChatClient turboChatClient;
    
    @Override
    public String chatText(String question) {
        return turboChatClient.prompt()
                .user(question)
                .call().content();
    }
    
}
