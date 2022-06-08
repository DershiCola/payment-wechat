package com.dershi.paymentwechat.controller;

import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.dershi.paymentwechat.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@CrossOrigin  // 开启前端跨域
@Api(tags = "订单管理")
@RestController
@RequestMapping("/api/order-info")
public class OrderInfoController {
    @Resource
    OrderInfoService orderInfoService;

    @GetMapping("/list")
    public R list() {
        List<OrderInfo> orderInfoList = orderInfoService.getAllOrderInfoByCreateTimeDesc();
        return R.ok().data("orderInfoList", orderInfoList);
    }
}
