package com.dershi.paymentwechat.task;

import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.entity.RefundInfo;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.dershi.paymentwechat.service.RefundInfoService;
import com.dershi.paymentwechat.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class WxPayTask {
    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    @Resource
    private WxPayService wxPayService;

    /*
    秒 分 时 日 月 周
    日和周不能同时指定:设置了日就不能设置周
    ?:不指定
     */
    /*
    以秒为例
    *:每秒都执行
    1-5:从第1秒开始执行到第5秒
    0/3:从第0秒开始每3秒执行一次
    1,2,3:在指定的第1、2、3秒执行
     */
    /**
     * 每30秒查一次，是否有创建时间超过5分钟且未支付的订单，如果有则进行关闭订单操作
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirm() throws Exception {
        log.info("orderConfirm 执行....");
        // 查询商户系统中创建时间超过5分钟且订单状态为"未支付"的所有订单
        List<OrderInfo> list = orderInfoService.getUnpaidOrdersByDuration(5);
        for (OrderInfo orderInfo : list) {
            // 调用微信平台的查询订单API确认订单的状态(可能支付成功了但通知商户失败，导致商户系统的订单状态未更新)
            log.info("创建时间超过5分钟且未支付的订单 => {}", orderInfo.getOrderNo());
            wxPayService.checkOrderStatus(orderInfo.getOrderNo());

        }
    }

    /**
     * 每30秒查一次，是否有创建时间超过5分钟且退款未成功的退款订单，如果有则进行关闭订单操作
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void refundConfirm() throws Exception {
        log.info("refundConfirm 执行....");
        // 查询商户系统中创建时间超过5分钟且退款状态为"退款处理中"的退款单
        List<RefundInfo> list = refundInfoService.getProcessingRefundsByDuration(5);
        for (RefundInfo refundInfo : list) {
            // 调用微信平台的查询订单API确认订单的状态(可能支付成功了但通知商户失败，导致商户系统的订单状态未更新)
            log.info("创建时间超过5分钟且仍处于退款中的退款单 => {}", refundInfo.getRefundNo());
            wxPayService.checkRefundStatus(refundInfo.getRefundNo());
        }
    }
}
