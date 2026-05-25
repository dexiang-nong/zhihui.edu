package com.tianji.aigc.agent;

import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
class RecommendWorkflowAgentTest {

    @Resource
    private RecommendWorkflowAgent recommendWorkflowAgent;

    @Test
    public void processStream() throws InterruptedException {
        String question = "推荐课程，20岁，本科，对java感兴趣";
        String sessionId = "a7ed4c24ea33492ba48fe25649fa873b";
        UserContext.setUser(2L);
        Flux<ChatEventVO> flux = recommendWorkflowAgent.processStream(question, sessionId);
        flux.subscribe(System.out::println);

        // 阻塞主线程，防止主线程结束，子线程终止
        Thread.sleep(100000);
    }

}