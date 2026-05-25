package com.tianji.aigc.config;

import com.alibaba.cloud.ai.memory.redis.LettuceRedisChatMemoryRepository;
import com.tianji.aigc.memory.mongo.MongoChatMemoryRepository;
import com.tianji.aigc.memory.mysql.JdbcChatMemoryRepository;
import com.tianji.aigc.tools.CourseTool;
import com.tianji.aigc.tools.OrderTool;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public ChatClient chatClient(
            ChatModel chatModel,
            Advisor simpleLoggerAdvisor,
            Advisor messageChatMemoryAdvisor,
            Advisor retrievalAugmentationAdvisor,
            SystemPromptConfig systemPromptConfig,
            CourseTool courseTool,
            OrderTool orderTool
    ) {
        return ChatClient.builder(chatModel)
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
    public Advisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }
    
}
