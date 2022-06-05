package com.dershi.paymentwechat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R {
    private Integer code; //响应码
    private String message; //响应消息
    private Map<String, Object> data = new HashMap<>(); //丰富的响应信息

    public R(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static R ok() {
        return new R(0, "成功");
    }

    public static R error() {
        return new R(-1, "失败");
    }

    public R data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
