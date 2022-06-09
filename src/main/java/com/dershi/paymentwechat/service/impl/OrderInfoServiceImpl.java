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
    public OrderInfo getOrderInfoByProductId(Long productId) {
        // 判断是否存在未支付的订单
        // 存在则返回，避免重复生成订单（暂未考虑用户ID的情形）
        OrderInfo orderInfo = getUnPaidOrderByProductId(productId);
        if (orderInfo != null) {
            // 若订单已过期，则删除，新建订单
            if (overdue(orderInfo.getCreateTime())) {
                deleteOrderInfoByOrderNo(orderInfo.getOrderNo());
            } else return orderInfo; // 未过期则直接返回
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
        // 订单信息写入数据库
        baseMapper.insert(orderInfo);

        return orderInfo;
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
     * 判断未支付的订单是否超过超过2小时（有效期）
     * @param createTime:订单生成日期
     * @return ture:已过期  false:在有效期内
     */
    private boolean overdue(Date createTime) {
        long time = createTime.getTime();
        long currentTime = System.currentTimeMillis();
        return (currentTime - time) >= 2L * 3600L * 1000L;
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
     * 获取未支付的订单
     * @param productId:商品ID
     * @return 未支付的订单对象
     */
    private OrderInfo getUnPaidOrderByProductId(Long productId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        return baseMapper.selectOne(queryWrapper);
    }

}
