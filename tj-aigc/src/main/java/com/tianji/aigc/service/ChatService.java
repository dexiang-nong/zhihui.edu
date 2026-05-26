package com.tianji.aigc.service;

import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import reactor.core.publisher.Flux;

/**
 * <p>
 * 聊天接口
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-22
 */
public interface ChatService {
    
    /**
     * 聊天响应
     */
    Flux<ChatEventVO> chat(String question, String sessionId);
    
    /**
     * 停止响应
     */
    void stop(String sessionId);
    
    static String getConversationId(String sessionId) {
        return UserContext.getUser() + "_" + sessionId;
    }
    
    /**
     * 文本聊天
     */
    String chatText(String question);
}
