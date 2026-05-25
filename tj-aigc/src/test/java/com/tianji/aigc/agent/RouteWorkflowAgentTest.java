package com.tianji.aigc.agent;

import cn.hutool.core.lang.Assert;
import com.tianji.aigc.enums.AgentTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RouteWorkflowAgentTest {

    @Resource
    private RouteWorkflowAgent routeWorkflowAgent;

    @Test
    public void testChat() throws InterruptedException {
        Assert.equals(
                routeWorkflowAgent.process("最新有哪些课程", "1"),
                AgentTypeEnum.RECOMMEND.getAgentName()
        );
        Assert.equals(
                routeWorkflowAgent.process("下单购买这个课程", "1"),
                AgentTypeEnum.BUY.getAgentName()
        );
        Assert.equals(
                routeWorkflowAgent.process("这个课程是多少钱", "1"),
                AgentTypeEnum.CONSULT.getAgentName()
        );
        Assert.equals(
                routeWorkflowAgent.process("java是什么", "1"),
                AgentTypeEnum.KNOWLEDGE.getAgentName()
        );
    }

}
