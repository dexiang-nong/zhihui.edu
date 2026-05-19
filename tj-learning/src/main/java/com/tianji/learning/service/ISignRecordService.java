package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignResultVO;

import java.util.List;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-28
 * @Description: 签到Service接口
 */
public interface ISignRecordService {
    
    /**
     * 签到
     */
    SignResultVO signRecords();
    
    /**
     * 查询本月签到记录
     */
    List<String> querySignRecord();
    
}
