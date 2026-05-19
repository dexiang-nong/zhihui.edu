package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningRecord;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-18
 */
public interface ILearningRecordService extends IService<LearningRecord> {
    
    /**
     * 查询学习记录
     */
    LearningLessonDTO queryLearningRecord(Long courseId);
    
    /**
     * 提交学习记录   <br>
     * 接口描述
     *  <ul>
     *      <li>视频：当前播放进度超过50%则判定本节学完</li>
     *      <li>考试：考试结束时提交记录，直接判定为本节学完</li>
     *  </ul>
     */
    void submitLearningRecord(LearningRecordFormDTO recordFormDTO);
}
