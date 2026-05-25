package com.tianji.aigc.agent;

import cn.hutool.core.date.DateUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.constants.ToolConstant;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.tools.CourseTool;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 课程咨询智能体
 */
@Component
@RequiredArgsConstructor
public class ConsultWorkflowAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;
    
    private final Advisor retrievalAugmentationAdvisor;
    
    private final CourseTool courseTool;

    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getConsultWorkflowAgentSystemMessage().get();
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.CONSULT;
    }

    @Override
    public List<Advisor> advisors() {
        // 添加RAG增强方案
        return List.of(retrievalAugmentationAdvisor);
    }

    @Override
    public Object[] tools() {
        return new Object[]{courseTool};
    }

    @Override
    public Map<String, Object> toolContext(String sessionId, String requestId) {
        Long userId = UserContext.getUser();
        return Map.of(
                ToolConstant.USER_ID, userId, // 设置用户id参数
                ToolConstant.REQUEST_ID, requestId  // 设置请求id参数
        );
    }

    @Override
    public Map<String, Object> systemMessageParams() {
        return Map.of("now", DateUtil.now());
    }
}
