package com.tianji.learning.utils;

import org.springframework.stereotype.Component;

@Component
public class TableInfoContext {
    
    private final ThreadLocal<String> TL = new ThreadLocal<>();

    public void setInfo(String info) {
        TL.set(info);
    }

    public String getInfo() {
        return TL.get();
    }

    public void remove() {
        TL.remove();
    }
    
}