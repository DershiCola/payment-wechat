package com.dershi.paymentwechat.service;

import java.util.Map;

/**
 * 返回订单号和二维码链接
 */
public interface WxPayService {
    Map<String, Object> nativePay(Long productId) throws Exception;
}
