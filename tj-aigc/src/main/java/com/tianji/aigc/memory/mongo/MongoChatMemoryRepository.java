package com.tianji.aigc.memory.mongo;

import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

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
public class MongoChatMemoryRepository implements MyChatMemoryRepository {
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
    
    public void optimization(String conversationId) {
        // 查询语句：根据对话id查询
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));
        // 修改语句：删除最后两条对话记录
        Update update = new Update();
        /*
            update.pop("messages", Update.Position.LAST);
            update.pop("messages", Update.Position.LAST);
            
            在 spring-data-mongodb 中，连续调用两次 update.pop，底层会将两个操作合并，仅仅删除了最后一条记录
         */
        update.pop("messages", Update.Position.LAST);
        mongoTemplate.updateFirst(query, update, MongoChatRecord.class);
        mongoTemplate.updateFirst(query, update, MongoChatRecord.class);
    }
}
