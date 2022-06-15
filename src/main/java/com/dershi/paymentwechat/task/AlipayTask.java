package com.dershi.paymentwechat.task;

import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.enums.PayType;
import com.dershi.paymentwechat.service.AliPayService;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.dershi.paymentwechat.service.RefundInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class AlipayTask {
    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    @Resource
    private AliPayService aliPayService;

    /**
     * 每30秒查一次，是否有创建时间超过5分钟且未支付的订单，如果有则进行关闭订单操作
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirm() throws Exception {
        log.info("支付宝订单 orderConfirm 执行....");
        // 查询商户系统中创建时间超过5分钟且订单状态为"未支付"的所有订单
        List<OrderInfo> list = orderInfoService.getUnpaidOrdersByDuration(5, PayType.ALIPAY.getType());
        for (OrderInfo orderInfo : list) {
            // 调用支付宝平台的查询订单API确认订单的状态(可能支付成功了但通知商户失败，导致商户系统的订单状态未更新)
            log.info("创建时间超过5分钟且未支付的订单 => {}", orderInfo.getOrderNo());
            aliPayService.checkOrderStatus(orderInfo.getOrderNo());
        }
    }
}
