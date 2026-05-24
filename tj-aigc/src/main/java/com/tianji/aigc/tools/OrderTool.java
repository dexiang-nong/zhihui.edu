package com.tianji.aigc.tools;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.constants.ToolConstant;
import com.tianji.aigc.tools.config.ToolResultHolder;
import com.tianji.aigc.tools.result.PrePlaceOrder;
import com.tianji.api.client.trade.TradeClient;
import com.tianji.api.dto.trade.OrderConfirmVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 订单 Tool Call
 * </p>
 *
 * @author dexiang.nong
 * @since 2026-05-24
 */
@Component
@RequiredArgsConstructor
public class OrderTool {
    
    private final TradeClient tradeClient;
    
    @Tool(description = "购买课程预下单操作")
    public PrePlaceOrder prePlaceOrder(
            @ToolParam(description = "课程id列表") List<Number> ids,
            ToolContext toolContext // chatClient传递的工具上下文参数
    ) {
        // 由于工具调用不是由主线程调用，所以无法获取主线程的用户id，从工具上下文中取出用户id，设置到此线程中
        UserContext.setUser(Convert.toLong(toolContext.getContext().get(ToolConstant.USER_ID)));
        // 大模型传入的ids，可能是int类型，所以转化为long类型，再调用Feign
        OrderConfirmVO orderConfirmVO = tradeClient.prePlaceOrder(CollStreamUtil.toList(ids, Number::longValue));
        
        return Optional.ofNullable(orderConfirmVO)
                .map(PrePlaceOrder::of)
                .map(prePlaceOrder -> {
                    // 字段: 类名 prePlaceOrder
                    String field = StrUtil.lowerFirst(prePlaceOrder.getClass().getSimpleName());
                    // 请求id: 从工具上下文获取
                    String requestId = Convert.toStr(toolContext.getContext().get(ToolConstant.REQUEST_ID));
                    // 存储到保持器中
                    ToolResultHolder.put(requestId, field, prePlaceOrder);
                    
                    return prePlaceOrder;
                })
                .orElse(null);
    }
    
}
