package com.dershi.paymentwechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.entity.RefundInfo;
import com.dershi.paymentwechat.enums.OrderStatus;
import com.dershi.paymentwechat.enums.wxpay.WxRefundStatus;
import com.dershi.paymentwechat.mapper.RefundInfoMapper;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.dershi.paymentwechat.service.RefundInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dershi.paymentwechat.util.OrderNoUtils;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {
    @Resource
    private OrderInfoService orderInfoService;

    @Override
    public RefundInfo createRefundInfoByOrderNo(String orderNo, String reason) {
        // 获取对应订单信息
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(orderNo);

        // 创建退款单信息
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo()); //退款单号
        refundInfo.setTotalFee(orderInfo.getTotalFee()); //原订单金额
        refundInfo.setRefund(orderInfo.getTotalFee()); //退款金额
        refundInfo.setReason(reason); //退款原因

        baseMapper.insert(refundInfo);

        return refundInfo;
    }

    @Override
    public void updateRefundInfo(String content) {
        Gson gson = new Gson();
        HashMap<String, String> map = gson.fromJson(content, HashMap.class);
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", map.get("out_refund_no"));

        // 设置修改的字段
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundId(map.get("refund_id")); //微信支付退款单号


        // 申请退款API和查询退款API的返回参数
        if (map.get("status") != null) {
            refundInfo.setRefundStatus(WxRefundStatus.valueOf(map.get("status")).getType()); //退款状态
            refundInfo.setContentReturn(content);
        }

        // 退款回调通知的参数
        if (map.get("refund_status") != null) {
            refundInfo.setRefundStatus(WxRefundStatus.valueOf(map.get("refund_status")).getType()); //退款状态
            refundInfo.setContentNotify(content);
        }

        baseMapper.update(refundInfo, queryWrapper);
    }

    @Override
    public String getRefundStatusByRefundNo(String refundNo) {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);
        return baseMapper.selectOne(queryWrapper).getRefundStatus();
    }

    @Override
    public void updateRefundStatusByRefundNo(String refundNo, String refundStatus) {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundStatus(refundStatus);

        baseMapper.update(refundInfo, queryWrapper);
    }

    @Override
    public List<RefundInfo> getProcessingRefundsByDuration(int minutes) {
        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_status", WxRefundStatus.PROCESSING.getType());
        queryWrapper.le("create_time", instant);
        return baseMapper.selectList(queryWrapper);
    }
}
