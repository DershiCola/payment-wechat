package com.dershi.paymentwechat.enums.alipay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商户与支付宝平台之间的交易状态
 */
@AllArgsConstructor
@Getter
public enum AliTradeStatus {

    /**
     * 支付成功
     */
    SUCCESS("TRADE_SUCCESS"),
    FINISHED("TRADE_FINISHED"),

    /**
     * 未支付
     */
    NOTPAY("WAIT_BUYER_PAY"),

    /**
     * 已关闭
     */
    CLOSED("TRADE_CLOSED");


    /**
     * 类型
     */
    private final String type;
}
