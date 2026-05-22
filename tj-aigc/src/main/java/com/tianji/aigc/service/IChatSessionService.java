package com.tianji.aigc.service;

import com.tianji.aigc.domain.po.ChatSession;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.domain.vo.SessionVO;

import java.util.List;

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
}
