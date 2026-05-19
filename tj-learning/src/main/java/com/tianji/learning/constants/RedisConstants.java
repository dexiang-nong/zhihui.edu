package com.tianji.learning.constants;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-28
 * @Description: Redis常量
 */
public interface RedisConstants {
    
    /**
     * 签到KEY前缀，如：sign:uid:123456:202504
     */
    String SIGN_RECORD_KEY_PREFIX = "sign:uid:";
    
    /**
     * 排行榜KEY前缀
     */
    String POINTS_BOARD_KEY_PREFIX = "boards:";
}
