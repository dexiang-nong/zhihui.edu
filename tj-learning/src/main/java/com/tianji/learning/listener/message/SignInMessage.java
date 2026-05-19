package com.tianji.learning.listener.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-28
 * @Description: 签到消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SignInMessage {
    
    private Long userId;
    
    private Integer points;
    
}
