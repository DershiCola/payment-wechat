package com.dershi.paymentwechat.service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


public interface WxPayService {
    /**
     * 返回订单号和二维码链接
     */
    Map<String, Object> nativePay(Long productId) throws Exception;

    /**
     * 应答通知和更新订单状态
     */
    void nativeNotify(HttpServletRequest request) throws Exception;

    /**
     * 取消订单
     */
    void cancelOrder(String orderNo) throws Exception;
}
