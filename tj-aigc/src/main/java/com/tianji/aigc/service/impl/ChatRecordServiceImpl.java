package com.tianji.aigc.service.impl;

import com.tianji.aigc.domain.po.ChatRecord;
import com.tianji.aigc.mapper.ChatRecordMapper;
import com.tianji.aigc.service.IChatRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 对话记录表 服务实现类
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-23
 */
@Service
public class ChatRecordServiceImpl extends ServiceImpl<ChatRecordMapper, ChatRecord> implements IChatRecordService {

}
