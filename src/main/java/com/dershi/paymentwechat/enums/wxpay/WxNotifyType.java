package com.dershi.paymentwechat.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类的API接口微信支付官方没有定义，需要开发人员自己实现
 */
@AllArgsConstructor
@Getter
public enum WxNotifyType {

	/**
	 * 支付通知
	 */
	NATIVE_NOTIFY("/api/wx-pay/native/notify"),


	/**
	 * 退款结果通知
	 */
	REFUND_NOTIFY("/api/wx-pay/refunds/notify");

	/**
	 * 类型
	 */
	private final String type;
}
