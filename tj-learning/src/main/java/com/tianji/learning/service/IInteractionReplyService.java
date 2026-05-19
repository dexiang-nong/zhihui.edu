package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;

/**
 * <p>
 * 互动问题的回答或评论 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
public interface IInteractionReplyService extends IService<InteractionReply> {
    
    /**
     * 新增回答或评论
     */
    void addReply(ReplyDTO replyDTO);
    
    /**
     * 分页查询回答或评论列表
     */
    PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query);
    
    /**
     * 管理端分页查询回答或评论列表
     */
    PageDTO<ReplyVO> queryAdminReplyPage(ReplyPageQuery query);
    
    /**
     * 管理端显示或隐藏评论
     */
    void showOrHiddenReply(Long id, boolean hidden);
    
    /**
     * 管理端根据id查询回答详情
     */
    ReplyVO queryAdminReplyById(Long id);
}
