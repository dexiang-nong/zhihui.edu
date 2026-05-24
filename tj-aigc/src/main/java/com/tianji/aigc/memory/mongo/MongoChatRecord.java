package com.tianji.aigc.memory.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * <p>
 * 会话记录结构
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_record") // 指定集合名称
public class MongoChatRecord {
    @Id
    private ObjectId id;
    
    @Indexed
    private String conversationId;
    
    private List<String> messages;
}
