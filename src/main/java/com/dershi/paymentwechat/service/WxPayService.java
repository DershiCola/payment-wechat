package com.dershi.paymentwechat.service;

import com.wechat.pay.contrib.apache.httpclient.exception.ParseException;
import com.wechat.pay.contrib.apache.httpclient.exception.ValidationException;
import com.wechat.pay.contrib.apache.httpclient.notification.Notification;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


public interface WxPayService {
    /**
     * 返回订单号和二维码链接
     */
    Map<String, Object> nativePay(Long productId) throws Exception;

    /**
     * 应答通知和更新订单状态
     */
    Notification nativeNotify(HttpServletRequest request) throws Exception;

    void processorOrder(Notification notification);
}
