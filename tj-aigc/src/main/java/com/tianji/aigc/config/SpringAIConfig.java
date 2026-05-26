package com.tianji.aigc.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.redis.LettuceRedisChatMemoryRepository;
import com.tianji.aigc.advisor.RecordOptimizationAdvisor;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import com.tianji.aigc.memory.mongo.MongoChatMemoryRepository;
import com.tianji.aigc.memory.mysql.JdbcChatMemoryRepository;
import com.tianji.aigc.tools.CourseTool;
import com.tianji.aigc.tools.OrderTool;
import com.tianji.common.constants.Constant;
import com.tianji.common.utils.WebUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;

import java.time.LocalDateTime;

/**
 * <p>
 * SpringAI配置
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-22
 */
@Configuration
public class SpringAIConfig {
    
    @Bean
    public ChatClient turboChatClient(
            @Qualifier("turboChatModel") ChatModel turboChatModel,
            Advisor simpleLoggerAdvisor,
            SystemPromptConfig systemPromptConfig
    ) {
        return ChatClient.builder(turboChatModel)
                .defaultSystem(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptConfig.getChatSystemMessage().get()))
                .defaultAdvisors(
                        simpleLoggerAdvisor     // 日志记录器
                )
                .build();
    }
    
    /*
        由【增强型智能体】改造为【路由工作流智能体】
        对于增强方案，由AbstractAgent的子类实现对应方法指定需要的增强方案
        
        优化：
            取消注册【会话记录器】，在 AbstractAgent#processStream 中独立注册
            因为需要调用process和processStream，导致存储两份记录，需要记录优化器
     */
    @Bean
    public ChatClient routeChatClient(
            @Qualifier("plusChatModel") ChatModel plusChatModel,
            Advisor simpleLoggerAdvisor
//            Advisor messageChatMemoryAdvisor,
//            Advisor recordOptimizationAdvisor
    ) {
        return ChatClient.builder(plusChatModel)
                .defaultAdvisors(
                        simpleLoggerAdvisor
//                        messageChatMemoryAdvisor, // 会话记录器
//                        recordOptimizationAdvisor // 记录优化器
                )
                .build();
    }
    
    // 【增强型智能体】
    @Bean
    public ChatClient baseChatClient(
            @Qualifier("plusChatModel") ChatModel plusChatModel,
            Advisor simpleLoggerAdvisor,
            Advisor messageChatMemoryAdvisor,
            Advisor retrievalAugmentationAdvisor,
            SystemPromptConfig systemPromptConfig,
            CourseTool courseTool,
            OrderTool orderTool
    ) {
        return ChatClient.builder(plusChatModel)
                .defaultAdvisors(
                        simpleLoggerAdvisor,         // 日志记录器
                        messageChatMemoryAdvisor,    // 会话记录器
                        retrievalAugmentationAdvisor // 向量数据库检索器
                )
                // 系统提示词 (手动从nacos上读取)
                .defaultSystem(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptConfig.getChatSystemMessage().get())
                        .param("now", LocalDateTime.now()))
                .defaultTools(
                        courseTool, // 课程工具
                        orderTool   // 订单工具
                )
                .build();
    }
    
    @Bean
    public ChatModel turboChatModel(DashScopeApi dashScopeApi) {
        DashScopeChatOptions dashScopeChatOptions = DashScopeChatOptions.builder()
                .model("qwen-turbo")
                .build();
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(dashScopeChatOptions)
                .build();
    }
    
    @Bean
    public ChatModel plusChatModel(DashScopeApi dashScopeApi) {
        DashScopeChatOptions dashScopeChatOptions = DashScopeChatOptions.builder()
                .model("qwen-plus")
                .build();
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(dashScopeChatOptions)
                .build();
    }
    
    @Bean
    public DashScopeApi dashScopeApi(@Value("${spring.ai.dashscope.api-key}") String apiKey) {
        return DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
    }
    
    @Bean
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
    
    @Bean
    public ChatMemory chatMemory(
            ChatMemoryRepository chatMemoryRepository,
            @Value("${tj.ai.memory.max:10}") int max
    ) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(max) // 上下文最大消息数量 (默认20)
                .build();
    }
    
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "redis")
    @Bean
    public ChatMemoryRepository lettuceRedisChatMemoryRepository(RedisProperties redisProperties) {
        return LettuceRedisChatMemoryRepository.builder()
                .host(redisProperties.getHost())
                .password(redisProperties.getPassword())
                .keyPrefix("chat:") // 默认 spring_ai_alibaba_chat_memory:
                .build();
    }
    
    // 官方提供的JdbcChatMemoryRepository自动注册
    
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "mysql")
    @Bean
    public ChatMemoryRepository jdbcChatMemoryRepository() {
        return new JdbcChatMemoryRepository();
    }

    
//    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "mongodb")
//    @Bean
//    public ChatMemoryRepository mongodbChatMemoryRepository(MongoTemplate mongoTemplate) {
//        return MongoChatMemoryRepository.builder().mongoTemplate(mongoTemplate).build();
//    }
    
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "mongodb")
    @Bean
    public ChatMemoryRepository mongodbChatMemoryRepository() {
        return new MongoChatMemoryRepository();
    }
    
    @Bean
    public Advisor retrievalAugmentationAdvisor(VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.6d) // 相似度阈值
                        .topK(6) // 返回最相似的6个文档
                        .build()
                )
                .build();
    }
    
    @Bean
    public Advisor recordOptimizationAdvisor(MyChatMemoryRepository myChatMemoryRepository) {
        return new RecordOptimizationAdvisor(myChatMemoryRepository);
    }
    
    @Bean
    public Advisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }
    
    /**
     * 创建并配置自定义重试监听器Bean
     * <p>
     * 实现说明：
     * 1. 创建匿名RetryListener实现，在重试操作期间管理Web属性
     * 2. 将监听器注册到提供的RetryTemplate实例
     *
     * @param retryTemplate Spring Retry模板对象，用于注册重试监听器
     * @return RetryListener 已注册到模板的重试监听器实例，将由Spring容器管理
     */
    @Bean
    public RetryListener customizeRetryTemplate(RetryTemplate retryTemplate) {
        // 创建自定义重试监听器，实现以下核心功能：
        // - 重试开始时设置上下文标识
        // - 重试结束后清理上下文标识
        RetryListener retryListener = new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                WebUtils.setAttribute(Constant.SPRING_AI_ATTR, Constant.SPRING_AI_FLAG);
                return true;
            }
            
            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                WebUtils.removeAttribute(Constant.SPRING_AI_ATTR);
            }
        };
        
        // 将监听器注册到重试模板
        retryTemplate.registerListener(retryListener);
        return retryListener;
    }
    
}
