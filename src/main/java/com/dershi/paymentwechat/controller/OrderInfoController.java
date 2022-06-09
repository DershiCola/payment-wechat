package com.dershi.paymentwechat.controller;

import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.enums.OrderStatus;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.dershi.paymentwechat.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@CrossOrigin  // 开启前端跨域
@Api(tags = "订单管理")
@RestController
@RequestMapping("/api/order-info")
public class OrderInfoController {
    @Resource
    OrderInfoService orderInfoService;

    /**
     * 查看所有订单
     * @return 订单List集合
     */
    @GetMapping("/list")
    public R list() {
        List<OrderInfo> orderInfoList = orderInfoService.getAllOrderInfoByCreateTimeDesc();
        return R.ok().data("orderInfoList", orderInfoList);
    }

    /**
     * 查看订单状态(前端根据订单状态是否支付成功关闭二维码)
     * @param orderNo:订单号
     * @return 传给前端的code和message，code:0表示支付成功,101表示正在支付...
     */
    @GetMapping("/query-order-status/{orderNo}")
    public R queryOrderStatus(@PathVariable String orderNo) {
        String status = orderInfoService.getOrderStatusByOrderNo(orderNo);
        if (OrderStatus.SUCCESS.getType().equals(status)) {
            return R.ok().setMessage(status);
        }
        return R.ok().setCode(101).setMessage(status);
    }

}
