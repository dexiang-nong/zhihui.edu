package com.tianji.aigc.memory.mongo;

import com.tianji.aigc.memory.MessageUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Mongo存储会话记录
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-23
 */
public class MongoChatMemoryRepository implements ChatMemoryRepository {
    @Resource
    private MongoTemplate mongoTemplate;
    
    @Override
    public List<String> findConversationIds() {
//        List<MongoChatRecord> list = mongoTemplate.findAll(MongoChatRecord.class);
//        return list.stream()
//                .map(MongoChatRecord::getConversationId)
//                .collect(Collectors.toList());
        return mongoTemplate.findDistinct(new Query(), "conversationId", MongoChatRecord.class, String.class);
    }
    
    @Override
    public List<Message> findByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));
        MongoChatRecord chatRecord = mongoTemplate.findOne(query, MongoChatRecord.class);
        if (chatRecord == null) {
            return List.of();
        }
        return chatRecord.getMessages().stream()
                .map(MessageUtil::toMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 删除原有数据
        deleteByConversationId(conversationId);
        // 保存最新数据
        MongoChatRecord chatRecord = MongoChatRecord.builder()
                .conversationId(conversationId)
                .messages(messages.stream().map(MessageUtil::toJson).toList())
                .build();
        mongoTemplate.save(chatRecord);
    }
    
    @Override
    public void deleteByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(query, MongoChatRecord.class);
    }
}
