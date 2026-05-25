package com.tianji.aigc.agent;

import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.AgentTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 路由工作流智能体【由路由类决定选择智能体】
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-25
 */
@Component
@RequiredArgsConstructor
public class RouteWorkflowAgent extends AbstractAgent {
    
    private final SystemPromptConfig systemPromptConfig;
    
    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getRouteWorkflowAgentSystemMessage().get();
    }
    
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }
    
}
