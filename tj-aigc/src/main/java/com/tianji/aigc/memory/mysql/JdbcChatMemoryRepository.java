package com.tianji.aigc.memory.mysql;

import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.domain.po.ChatRecord;
import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.service.IChatRecordService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Mysql存储会话记录
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-23
 */
public class JdbcChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private IChatRecordService chatRecordService;

    @Override
    public List<String> findConversationIds() {
        List<ChatRecord> list = chatRecordService.lambdaQuery()
                .select(ChatRecord::getConversationId)
                .list();
        return list.stream()
                .map(ChatRecord::getConversationId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatRecord> list = chatRecordService.lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByDesc(ChatRecord::getCreateTime)
                .list();
        return list.stream()
                .map(record -> MessageUtil.toMessage(record.getData()))
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 删除原始记录
        deleteByConversationId(conversationId);
        // 保存最新记录
        String userId = StrUtil.subBefore(conversationId, "_", false);
        List<ChatRecord> list = messages.stream()
                .map(message -> ChatRecord.builder()
                        .conversationId(conversationId)
                        .data(MessageUtil.toJson(message))
                        .creater(Long.parseLong(userId))
                        .updater(Long.parseLong(userId))
                        .build())
                .collect(Collectors.toList());
        chatRecordService.saveBatch(list);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        chatRecordService.lambdaUpdate()
                .eq(ChatRecord::getConversationId, conversationId)
                .remove();
    }
}
