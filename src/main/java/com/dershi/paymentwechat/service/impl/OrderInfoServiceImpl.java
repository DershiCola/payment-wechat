package com.dershi.paymentwechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.entity.Product;
import com.dershi.paymentwechat.enums.OrderStatus;
import com.dershi.paymentwechat.mapper.OrderInfoMapper;
import com.dershi.paymentwechat.mapper.ProductMapper;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dershi.paymentwechat.util.OrderNoUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private ProductMapper productMapper;

    /**
     * 根据商品ID查询订单，若订单不存在则进行创建
     * 若订单已存在，未过期则返回;已过期则新建订单
     * @param productId:商品ID
     * @return 订单对象
     */
    @Override
    public OrderInfo getOrderInfoByProductId(Long productId, String paymentType) {
        // 判断是否存在未支付的订单
        // 存在则返回，避免重复生成订单（暂未考虑用户ID的情形）
        OrderInfo orderInfo = getUnPaidOrderByProductId(productId, paymentType);
        if (orderInfo != null) {
            return orderInfo; // 未过期则直接返回
        }

        // 获取商品信息
        Product product = productMapper.selectById(productId);

        // 创建订单信息
        orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType(paymentType);
        // 订单信息写入数据库
        baseMapper.insert(orderInfo);

        return orderInfo;
    }

    /**
     * 根据订单号查询订单信息
     * @param orderNo:订单号
     * @return 订单信息对象
     */
    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 保存二维码链接
     * @param orderNo:订单号
     * @param codeUrl:二维码链接
     */
    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        // 设置二维码链接
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);

        // 更新二维码链接字段
        baseMapper.update(orderInfo, queryWrapper);
    }

    /**
     * 根据订单号删除单个订单
     * @param orderNo:订单号
     */
    @Override
    public void deleteOrderInfoByOrderNo(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        baseMapper.delete(queryWrapper);
    }

    /**
     * 按创建时间倒序获取所有订单信息
     * @return 订单信息List集合
     */
    @Override
    public List<OrderInfo> getAllOrderInfoByCreateTimeDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<OrderInfo>().orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据订单号更新订单的订单状态
     * @param orderNo:订单编号
     * @param orderStatus:订单状态
     */
    @Override
    public void updateOrderStatusByOrderNo(String orderNo, String orderStatus) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(orderStatus);

        baseMapper.update(orderInfo, queryWrapper);
    }

    /**
     * 根据订单号获取订单状态
     * @param orderNo:订单号
     * @return 订单状态
     */
    @Override
    public String getOrderStatusByOrderNo(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);
        if (orderInfo == null) {
            return null;
        }
        return orderInfo.getOrderStatus();
    }

    /**
     * 查询创建时间超过minutes分钟且未支付的订单
     * @param minutes:分钟
     * @return 超时未支付的订单
     */
    @Override
    public List<OrderInfo> getUnpaidOrdersByDuration(int minutes, String paymentType) {
        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        queryWrapper.eq("payment_type", paymentType);
        queryWrapper.le("create_time", instant);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 获取未支付的订单
     * @param productId:商品ID
     * @return 未支付的订单对象
     */
    private OrderInfo getUnPaidOrderByProductId(Long productId, String paymentType) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("payment_type", paymentType);
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        return baseMapper.selectOne(queryWrapper);
    }

}
