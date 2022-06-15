package com.dershi.paymentwechat.service;

import java.util.Map;

public interface AliPayService {
    String tradeCreate(Long productId) throws Exception;
    String tradeNotify(Map<String, String> params);
    void cancelOrder(String orderNo) throws Exception;
    String queryOrder(String orderNo) throws Exception;
    String queryRefund(String orderNo) throws Exception;
    void checkOrderStatus(String orderNo) throws Exception;
    void refunds(String orderNo, String reason) throws Exception;
    String downloadBill(String billDate, String billType) throws Exception;
}
