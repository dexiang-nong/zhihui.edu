package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-21
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {
    
    /**
     * 新增互动问题
     */
    void addQuestion(QuestionFormDTO questionFormDTO);
    
    /**
     * 修改互动问题
     */
    void updateQuestion(Long id, QuestionFormDTO questionFormDTO);
    
    /**
     * 用户端分页查询问题
     */
    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);
    
    /**
     * 根据id查询问题详情
     */
    QuestionVO queryQuestionById(Long id);
    
    /**
     * 删除我的问题
     */
    void removeMyQuestion(Long id);
    
    /**
     * 管理端分页查询问题
     */
    PageDTO<QuestionAdminVO> queryAdminQuestionPage(QuestionAdminPageQuery query);
    
    /**
     * 管理端隐藏或显示问题
     */
    void showOrHiddenQuestion(Long id, boolean hidden);
    
    /**
     * 管理端根据id查询问题详情
     */
    QuestionAdminVO queryAdminQuestionById(Long id);
}
