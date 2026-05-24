package com.tianji.aigc.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
public class ChatServiceImpl implements ChatService {
    
    private final ChatClient chatClient;
    
    private final ChatMemory chatMemory;
    
    /*
        存储大模型的生成状态
            1) 单机环境: 采用ConcurrentHashMap确保线程安全
            2) 分布式环境: 采用第三方平台（如Redis）确保线程安全
     */
    private final StringRedisTemplate redisTemplate;
    private static final String GENERATE_STATUS_KEY = "generate_status";
    
    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        String conversationId = ChatService.getConversationId(sessionId);
        
        StringBuilder outputBuilder = new StringBuilder();
        
        BoundSetOperations<String, String> setOperations = redisTemplate.boundSetOps(GENERATE_STATUS_KEY);
        
        return chatClient.prompt()
                .user(question)
                // 指定会话
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 流式相应
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
                    String text = chatResponse.getResult().getOutput().getText();
                    // 拼接AI生成内容
                    outputBuilder.append(text);
                    // 封装VO对象输出
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                // 标记输出结束
                .concatWith(Flux.just(ChatEventVO.builder()
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }
    
    @Override
    public void stop(String sessionId) {
        redisTemplate.opsForSet().remove(GENERATE_STATUS_KEY, sessionId);
    }
    
//    private static final Set<String> GENERATE_STATUS = new ConcurrentHashSet<>();
//
//    @Override
//    public Flux<ChatEventVO> chat(String question, String sessionId) {
//        String conversationId = ChatService.getConversationId(sessionId);
//
//        StringBuilder outputBuilder = new StringBuilder();
//
//        return chatClient.prompt()
//                .user(question)
//                // 指定会话
//                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
//                // 流式相应
//                .stream().chatResponse()
//                // 记录会话状态
//                .doFirst(() -> GENERATE_STATUS.add(sessionId))             // 初始，添加标识
//                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId)) // 异常，删除标识
//                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId))     // 完成，删除标识
//                .doOnCancel(() -> // 当输出被取消时，将AI生成的内容存入到会话中
//                        saveAIOutput(conversationId, outputBuilder.toString()))
//                .takeWhile(response -> // 通过返回值控制Flux流是否继续
//                        GENERATE_STATUS.contains(sessionId))
//                // 格式化输出
//                .map(chatResponse -> {
//                    String text = chatResponse.getResult().getOutput().getText();
//                    // 拼接AI生成内容
//                    outputBuilder.append(text);
//                    // 封装VO对象输出
//                    return ChatEventVO.builder()
//                            .eventData(text)
//                            .eventType(ChatEventTypeEnum.DATA.getValue())
//                            .build();
//                })
//                // 标记输出结束
//                .concatWith(Flux.just(ChatEventVO.builder()
//                        .eventType(ChatEventTypeEnum.STOP.getValue())
//                        .build()));
//    }
//
//    @Override
//    public void stop(String sessionId) {
//        GENERATE_STATUS.remove(sessionId); // 删除标识
//    }
    
    private void saveAIOutput(String conversationId, String output) {
        chatMemory.add(conversationId, new AssistantMessage(output));
    }
}
