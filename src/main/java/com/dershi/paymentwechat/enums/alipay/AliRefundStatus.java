package com.dershi.paymentwechat.enums.alipay;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AliRefundStatus {
    REFUND_SUCCESS("退款成功"),

    REFUND_ERROR("退款失败");

    /**
     * 类型
     */
    private final String type;
}
