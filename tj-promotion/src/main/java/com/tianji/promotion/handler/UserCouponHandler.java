package com.tianji.promotion.handler;

import cn.hutool.core.collection.CollUtil;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.DateUtils;
import com.tianji.message.api.client.AsyncSmsClient;
import com.tianji.message.domain.dto.SmsInfoDTO;
import com.tianji.promotion.constants.RedisConstant;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.properties.SmsRemindProperties;
import com.tianji.promotion.service.IUserCouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author 张德帅
 * @Version 1.0
 * @CreateTime: 2025-05-12
 * @Description: 用户优惠卷定时处理
 */
//@Component
@RequiredArgsConstructor
public class UserCouponHandler {
    
    private final IUserCouponService userCouponService;
    
    private final SmsRemindProperties smsRemindProperties;
    
    private final StringRedisTemplate redisTemplate;
    
    private final UserClient userClient;
    
    private final AsyncSmsClient asyncSmsClient;
    
    /**
     * 短信提醒用户优惠卷过期
     */
    @XxlJob("SmsRemindUserCouponTernJob")
    public void smsRemindUserCouponTernJob() {
        // 定义分页参数
        int shardIndex = XxlJobHelper.getShardIndex(); // 获取执行器编号
        int shardTotal = XxlJobHelper.getShardTotal(); // 获取执行器数量
        int pageNo = shardIndex + 1; // 起始号就是分片序号+1
        int pageSize = 1000;
        // 循环处理
        while (true) {
            // 1. 查询即将过期的用户优惠卷
            int timeout = smsRemindProperties.getTimeout();
            ChronoUnit unit = smsRemindProperties.parseChronoUnit();
            List<UserCoupon> UserCouponList = userCouponService.queryWillExpireCoupons(
                    timeout, unit,
                    pageNo, pageSize
            );
            if (CollUtil.isEmpty(UserCouponList)) {
                break;
            }
            // 2. 查询用户电话号码
            Set<Long> userIds = UserCouponList.stream()
                    .map(UserCoupon::getUserId)
                    .collect(Collectors.toSet());
            List<UserDTO> userList = userClient.queryUserByIds(userIds);
            Map<Long, String> userPhoneMap = userList.stream()
                    .collect(Collectors.toMap(UserDTO::getId, UserDTO::getCellPhone));
            // 3. 发送短信
            SmsInfoDTO smsInfoDTO = new SmsInfoDTO();
            /*
                模板：
                    尊敬的用户，您好！您有一张有效期至${expireDate}的优惠券尚未使用。
                    优惠不容错过，请尽快登录天机学堂使用。如有疑问，请联系客服。祝您购物愉快！
                备注: 因为添加模板需要企业认证, 所以无法添加, 只能使用系统默认的模板, 将${code}替换为上面内容
             */
            // TODO: 无法实现模板替换
            String format = "尊敬的用户，您好！您有一张有效期至%s的优惠券尚未使用。\n"
                    + "优惠不容错过，请尽快登录天机学堂使用。如有疑问，请联系客服。祝您购物愉快！";
            smsInfoDTO.setTemplateCode("SMS_308685084");
            Map<String, String> map = new HashMap<>();
            for (UserCoupon userCoupon : UserCouponList) {
                // 3.1. 判断是否发送过（如果SETNX失败，代表已经发送过）
                String key = RedisConstant.SMS_REMIND_KEY_PREFIX + userCoupon.getId();
                Boolean success = redisTemplate.opsForValue().setIfAbsent(
                        key, "1",
                        timeout, TimeUnit.of(unit)
                );
                if (Boolean.FALSE.equals(success)) {
                    continue;
                }
                // 3.2. 设置手机号
                smsInfoDTO.setPhones(List.of(userPhoneMap.get(userCoupon.getUserId())));
                // 3.3. 设置模板参数: yyyy-MM-dd HH:mm:ss
                String text = String.format(
                        format,
                        userCoupon.getTermEndTime()
                                .format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_DATE_TIME_FORMAT))
                );
                map.put("code", text);
                smsInfoDTO.setTemplateParams(map);
                // 3.4. 发送短信
                asyncSmsClient.sendMessage(smsInfoDTO);
            }
            // 4. 翻页
            pageNo += shardTotal;
        }
    }
    
}
