package com.tianji.aigc.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.aigc.constants.ToolConstant;
import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.memory.mongo.MongoChatRecord;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.tools.config.ToolResultHolder;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * <p>
 * 路由工作流智能体【抽象类，包含公共逻辑】
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-25
 */
public abstract class AbstractAgent implements Agent {
    
    /*
        根据ChatServiceImpl：增强型智能体改造
     */
    
    @Resource
    private ChatClient routeChatClient;
    
    @Resource
    private ChatMemory chatMemory;
    
    /*
        存储大模型的生成状态
            1) 单机环境: 采用ConcurrentHashSet确保线程安全
            2) 分布式环境: 采用第三方平台（如Redis:Set）确保线程安全
     */
    // 因为是使用 @Resource 注入，所以名称不可以是 redisTemplate
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String GENERATE_STATUS_KEY = "generate_status";
    
    // 输出结束的标记
    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue())
            .build();
    
    @Resource
    private Advisor messageChatMemoryAdvisor;
    
//    @Resource
//    private ChatMemoryRepository chatMemoryRepository;
    
    @Resource
    private MongoTemplate mongoTemplate;
    
    @Override
    public String process(String question, String sessionId) {
        // 设置工具上下文参数
        String requestId = IdUtil.fastSimpleUUID();
        
        // call() 阻塞调用
        // 1. 这里没有注入【会话记录器】，所以手动获取会话记录
        String conversationId = ChatService.getConversationId(sessionId);
//        List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);
//        if (CollUtil.isNotEmpty(messages)) {
//            messages.add(new UserMessage(question));
//            List<String> list = messages.stream()
//                    .map(MessageUtil::toJson)
//                    .toList();
//            question = JSONUtil.toJsonStr(list);
//        }
        // findByConversationId 里面将json => Message, 上面又Message=>json；下面直接查询
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));
        MongoChatRecord chatRecord = mongoTemplate.findOne(query, MongoChatRecord.class);
        if (chatRecord != null) {
            chatRecord.getMessages().add(question);
            question = JSONUtil.toJsonStr(chatRecord.getMessages());
        }
        // 2. 将全部会话记录一起发送给大模型
        return getChatClientRequest(sessionId, requestId, question)
                .call().content();
    }
    
    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        // 拼接对话id: userId_sessionId
        String conversationId = ChatService.getConversationId(sessionId);
        // 存储AI生成内容
        StringBuilder outputBuilder = new StringBuilder();
        // 绑定redis:set
        BoundSetOperations<String, String> setOperations = stringRedisTemplate.boundSetOps(GENERATE_STATUS_KEY);
        // 工具上下文参数
        String requestId = IdUtil.fastSimpleUUID();
        
        // 流式响应
        return getChatClientRequest(sessionId, requestId, question)
                // 独自添加会话记录器，这样就不用【记录优化器了】
                .advisors(messageChatMemoryAdvisor)
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
    
    private ChatClient.ChatClientRequestSpec getChatClientRequest(
            String sessionId, String requestId, String question) {
        return routeChatClient.prompt()
                .user(question)
                // 每个智能体会有自己的系统提示词
                .system(promptSystemSpec -> promptSystemSpec
                        .text(systemMessage())
                        .params(systemMessageParams()))
                // advisor需要在这里指定了，因为有的智能体不需要RAG增强
                .advisors(advisor -> advisor
                        .advisors(advisors())
                        .params(advisorParams(sessionId, requestId)))
                // tool也是，有的智能体不需要调用某个工具
                .tools(tools())
                // 工具上下文参数
                .toolContext(toolContext(sessionId, requestId));
    }
    
    private void saveAIOutput(String conversationId, String output) {
        chatMemory.add(conversationId, new AssistantMessage(output));
    }
    
    @Override
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        var conversationId = ChatService.getConversationId(sessionId);
        return Map.of(ChatMemory.CONVERSATION_ID, conversationId);
    }
    
    @Override
    public void stop(String sessionId) {
        stringRedisTemplate.opsForSet().remove(GENERATE_STATUS_KEY, sessionId);
    }
    
}
