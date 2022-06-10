package com.dershi.paymentwechat.service;

import com.dershi.paymentwechat.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {
    OrderInfo getOrderInfoByProductId(Long productId);
    OrderInfo getOrderInfoByOrderNo(String orderNo);
    void saveCodeUrl(String orderNo, String codeUrl);
    void deleteOrderInfoByOrderNo(String orderNo);
    List<OrderInfo> getAllOrderInfoByCreateTimeDesc();
    void updateOrderStatusByOrderNo(String orderNo, String orderStatus);
    String getOrderStatusByOrderNo(String orderNo);
    List<OrderInfo> getUnpaidOrdersByDuration(int minutes);
}
