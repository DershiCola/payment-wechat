package com.dershi.paymentwechat.service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


public interface WxPayService {
    /**
     * 返回订单号和二维码链接
     */
    Map<String, Object> nativePay(Long productId) throws Exception;

    /**
     * 应答支付通知和更新订单状态
     */
    void nativeNotify(HttpServletRequest request) throws Exception;

    /**
     * 取消订单
     */
    void cancelOrder(String orderNo) throws Exception;

    /**
     * 调用微信平台API查询订单信息
     */
    String queryOrder(String orderNo) throws Exception;

    /**
     * 调用微信查单API确认订单状态
     */
    void checkOrderStatus(String orderNo) throws Exception;

    /**
     * 订单退款
     */
    void refunds(String orderNo, String reason) throws Exception;

    /**
     * 应答退款通知和更新退款单状态
     */
    void refundsNotify(HttpServletRequest request) throws Exception;

    /**
     * 调用微信平台API查询退款单信息
     */
    String queryRefund(String refundNo) throws Exception;

    /**
     * 调用微信查询退款API确认退款状态
     */
    void checkRefundStatus(String refundNo) throws Exception;

    String queryBill(String billDate, String billType) throws Exception;

    String downloadBill(String billDate, String billType) throws Exception;
}
