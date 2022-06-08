package com.dershi.paymentwechat.service;

import com.dershi.paymentwechat.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {
    OrderInfo getOrderInfoByProductId(Long productId);
    void saveCodeUrl(String orderNo, String codeUrl);
    void deleteOrderInfoByOrderNo(String orderNo);
    List<OrderInfo> getAllOrderInfoByCreateTimeDesc();
    void updateOrderStatusByOrderNo(String orderNo, String orderStatus);
}
