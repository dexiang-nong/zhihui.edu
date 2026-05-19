package com.tianji.remark.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.remark.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-24
 */
public interface ILikedRecordService extends IService<LikedRecord> {
    
    /**
     * 点赞或取消点赞
     */
    void addLikeRecord(LikeRecordFormDTO recordDTO);
    
    /**
     * 批量查询点赞状态（查询当前用户是否点赞了指定的业务）
     */
    Set<Long> queryLikedStatus(List<Long> bizIds);
    
    void readLikedTimesAndSendMessage();
    
    void persistenceLikedRecord();
}
