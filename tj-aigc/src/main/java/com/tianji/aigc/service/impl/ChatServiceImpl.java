package com.tianji.aigc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.constants.ToolConstant;
import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.IChatSessionService;
import com.tianji.aigc.tools.config.ToolResultHolder;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * <p>
 * 聊天接口实现
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-22
 */
@Service
@RequiredArgsConstructor
// 该服务存在两个智能体方案：【增强型智能体：ENHANCE】【路由工作流智能体：ROUTE】，只能使用一个，所以添加条件
@ConditionalOnProperty(prefix = "tj.ai", value = "chat-type", havingValue = "ENHANCE")
public class ChatServiceImpl implements ChatService {
    
    private final ChatClient baseChatClient;
    
    private final ChatMemory chatMemory;
    
    /*
        存储大模型的生成状态
            1) 单机环境: 采用ConcurrentHashSet确保线程安全
            2) 分布式环境: 采用第三方平台（如Redis:Set）确保线程安全
     */
    private final StringRedisTemplate redisTemplate;
    private static final String GENERATE_STATUS_KEY = "generate_status";
    
    // 输出结束的标记
    private static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue())
            .build();
    
    private final IChatSessionService chatSessionService;
    
    private final ChatClient turboChatClient;
    
    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // 拼接对话id: userId_sessionId
        String conversationId = ChatService.getConversationId(sessionId);
        // 存储AI生成内容
        StringBuilder outputBuilder = new StringBuilder();
        // 绑定redis:set
        BoundSetOperations<String, String> setOperations = redisTemplate.boundSetOps(GENERATE_STATUS_KEY);
        // 工具上下文参数
        String requestId = IdUtil.fastSimpleUUID();
        Long userId = UserContext.getUser();
        
        // 更新会话标题
        String title = StrUtil.sub(question, 0, 100);
        chatSessionService.asyncUpdateHistorySessionTitle(sessionId, title);
        
        return baseChatClient.prompt()
                .user(question)
                // 指定会话
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 传递工具上下文参数
                .toolContext(MapUtil.<String, Object>builder()
                        .put(ToolConstant.REQUEST_ID, requestId)
                        .put(ToolConstant.USER_ID, userId)
                        .build())
                // 流式响应
                .stream().chatResponse()
                // 记录会话状态
                .doFirst(() -> setOperations.add(sessionId))             // 初始，添加标识
                .doOnError(throwable -> setOperations.remove(sessionId)) // 异常，删除标识
                .doOnComplete(() -> setOperations.remove(sessionId))     // 完成，删除标识
                .doOnCancel(() -> // 当输出被取消时，将AI生成的内容存入到会话中
                        saveAIOutput(conversationId, outputBuilder.toString()))
                .takeWhile(response -> // 通过返回值控制Flux流是否继续
                        BooleanUtil.isTrue(setOperations.isMember(sessionId)))
                // 格式化输出
                .map(chatResponse -> {
                    /*
                        如果是最后一条数据，将messageId存入内存中
                        在存入数据库时，通过messageId取出params，一并存入数据库
                     */
                    String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals(ToolConstant.STOP, finishReason)) {
                        String messageId = chatResponse.getMetadata().getId();
                        ToolResultHolder.put(messageId, ToolConstant.REQUEST_ID, requestId);
                    }
                    // 拼接AI生成内容
                    String text = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(text);
                    // 封装VO对象输出
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                // 标记输出结束（判断是否携带参数：实现课程卡片）
                .concatWith(Flux.defer(() -> {
                    // 通过请求id获取到参数列表，如果不为空，就将其追加到返回结果中
                    Map<String, Object> map = ToolResultHolder.get(requestId);
                    if (CollUtil.isNotEmpty(map)) {
                        ToolResultHolder.remove(requestId); // 清除参数列表
                        // 响应给前端的参数数据
                        ChatEventVO chatEventVO = ChatEventVO.builder()
                                .eventData(map)
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .build();
                        return Flux.just(chatEventVO, STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }
    
    @Override
    public void stop(String sessionId) {
        redisTemplate.opsForSet().remove(GENERATE_STATUS_KEY, sessionId);
    }
    
    @Override
    public String chatText(String question) {
        return turboChatClient.prompt()
                .user(question)
                .call().content();
    }
    
    private void saveAIOutput(String conversationId, String output) {
        chatMemory.add(conversationId, new AssistantMessage(output));
    }
}
