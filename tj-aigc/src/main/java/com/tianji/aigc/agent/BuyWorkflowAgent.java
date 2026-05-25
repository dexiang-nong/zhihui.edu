package com.tianji.aigc.agent;

import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.constants.ToolConstant;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.tools.OrderTool;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <p>
 * 购买工作流智能体
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-25
 */
@Component
@RequiredArgsConstructor
public class BuyWorkflowAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;
    
    private final OrderTool orderTool;

    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getBuyWorkflowAgentSystemMessage().get();
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.BUY;
    }

    @Override
    public Object[] tools() {
        return new Object[]{orderTool};
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        Long userId = UserContext.getUser();
        return Map.of(
                ToolConstant.USER_ID, userId, // 设置用户id参数
                ToolConstant.REQUEST_ID, requestId  // 设置请求id参数
        );
    }
}
