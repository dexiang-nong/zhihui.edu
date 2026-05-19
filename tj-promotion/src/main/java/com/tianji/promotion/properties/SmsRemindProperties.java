package com.tianji.promotion.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-12
 * @Description: Sms提醒配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "tj.promotion")
public class SmsRemindProperties {
    
    /**
     * 优惠卷还剩多少时间过期
     */
    private int timeout;
    
    /**
     * 时间单位（支持：SECONDS, MINUTES, HOURS, DAYS）
     */
    private String timeUnit;
    
    public ChronoUnit parseChronoUnit() {
        return ChronoUnit.valueOf(this.timeUnit.toUpperCase());
    }
    
}
