package com.tianji.aigc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.domain.po.ChatSession;
import com.tianji.aigc.domain.vo.MessageVO;
import com.tianji.aigc.domain.vo.SessionVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 对话session 服务类
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-22
 */
public interface IChatSessionService extends IService<ChatSession> {
    
    /**
     * 新建会话
     */
    SessionVO createSession(Integer num);
    
    /**
     * 获取热门会话
     */
    List<SessionVO.Example> hotExamples(Integer num);
    
    /**
     * 根据会话id查询消息列表
     */
    List<MessageVO> queryBySessionId(String sessionId);
    
    /**
     * 查询历史会话
     */
    Map<String, List<SessionVO>> queryHistorySession();
    
    /**
     * 更新历史会话标题
     */
    void putHistorySessionTitle(String sessionId, String title);
    
    /**
     * 删除历史会话
     */
    void deleteHistorySession(String sessionId);
}
