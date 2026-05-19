package com.tianji.learning.config;

import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MybatisConfiguration {
    
    private final TableInfoContext tableInfoContext;

//    @Bean
//    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {
//        // 准备一个Map，用于存储TableNameHandler
//        Map<String, TableNameHandler> map = new HashMap<>(1);
//        /*
//            存入一个TableNameHandler，用来替换points_board表名称
//            替换方式，就是从TableInfoContext中读取保存好的动态表名
//         */
//        map.put("points_board",
//                (sql, tableName) -> tableInfoContext.getInfo() == null
//                        ? tableName
//                        : tableInfoContext.getInfo()
//        );
//        map.put("points_record",
//                (sql, tableName) -> tableInfoContext.getInfo() == null
//                        ? tableName
//                        : tableInfoContext.getInfo()
//        );
//        return new DynamicTableNameInnerInterceptor(map);
//    }
    
    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {
        return new DynamicTableNameInnerInterceptor((sql, tableName) -> {
            if ("points_board".equals(tableName)
                    || "points_record".equals(tableName)) {
                return tableInfoContext.getInfo();
            }
            return tableName;
        });
    }
    
}