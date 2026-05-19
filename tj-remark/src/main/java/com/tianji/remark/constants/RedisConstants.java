package com.tianji.remark.constants;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-04-26
 * @Description: Redis常量
 */
public interface RedisConstants {
    
    /**
     * 缓存用户点赞的KEY点缀
     */
    String LIKES_BIZ_KEY_PREFIX = "likes:set:biz:";
    
    /**
     * 缓存业务要点赞次数的KEY前缀
     */
    String LIKES_TIMES_KEY_PREFIX = "likes:times:type:";
}
