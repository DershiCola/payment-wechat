package com.dershi.paymentwechat.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum WxRefundStatus {

    /**
     * 退款成功
     */
    SUCCESS("退款成功"),

    /**
     * 退款关闭
     */
    CLOSED("退款关闭"),

    /**
     * 退款处理中
     */
    PROCESSING("退款处理中"),

    /**
     * 退款异常
     */
    ABNORMAL("退款异常");

    /**
     * 类型
     */
    private final String type;
}
