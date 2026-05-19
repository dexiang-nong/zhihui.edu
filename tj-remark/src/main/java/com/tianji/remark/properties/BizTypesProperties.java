package com.tianji.remark.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "tj.remark")
public class BizTypesProperties {
    /**
     * 点赞业务类型
     */
	private List<String> bizTypes;
}