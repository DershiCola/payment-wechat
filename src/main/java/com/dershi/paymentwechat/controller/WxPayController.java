package com.dershi.paymentwechat.controller;

import com.dershi.paymentwechat.service.WxPayService;
import com.dershi.paymentwechat.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网页微信支付API")
@Slf4j
public class WxPayController {
    @Resource
   private WxPayService wxPayService;

    @ApiOperation("调用Native统一下单API，生成二维码链接")
    @PostMapping("/native/{productId}")
    public R nativePay(@PathVariable Long productId) {
        log.info("发起支付请求");
        Map<String, Object> map;
        try {
            map = wxPayService.nativePay(productId);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error().setMessage("下单异常");
        }
        return R.ok().setData(map);
    }

    @ApiOperation("支付通知")
    @PostMapping("/native/notify")
    public R nativeNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            wxPayService.nativeNotify(request);
            // 验签没有抛出异常，说明验签成功
            log.info("支付通知验签成功");

            // 模拟响应超时，微信支付平台会不断重复通知
//            TimeUnit.SECONDS.sleep(5);

            // 接受且验签成功:HTTP应答状态码需返回200或204
            response.setStatus(200);
            return R.ok().setMessage("支付通知验签成功");
        } catch (Exception e) {
            // 接受或验签失败:HTTP应答状态码需返回5XX或4XX，同时需返回应答报文
            log.info("支付通知验签失败");
            response.setStatus(500);
            return R.error().setMessage("支付通知验签失败");
        }
    }

    @ApiOperation("取消订单")
    @PostMapping("/cancel/{orderNo}")
    public R cancelOrder(@PathVariable String orderNo) {
        log.info("取消订单 = {}", orderNo);
        try {
            wxPayService.cancelOrder(orderNo);
            return R.ok().setMessage("成功取消订单");
        } catch (Exception e) {
            e.printStackTrace();
            return R.error().setMessage("取消订单失败");
        }
    }

    @ApiOperation("查询订单")
    @GetMapping("/query-order/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) {
        try {
            String result = wxPayService.queryOrder(orderNo);
            return R.ok().setMessage("查询成功").data("result", result);
        } catch (Exception e) {
            return R.error().setMessage("查询失败").data("errorReason", e.getMessage());
        }
    }

    @ApiOperation("订单退款")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) {
        try {
            wxPayService.refunds(orderNo, reason);
            return R.ok().setMessage("退款成功");
        } catch (Exception e) {
            return R.error().setMessage("退款失败").data("errorReason", e.getMessage());
        }
    }

    @ApiOperation("退款通知")
    @PostMapping("/refunds/notify")
    public R refundsNotify(HttpServletRequest request, HttpServletResponse response) {

        try {
            wxPayService.refundsNotify(request);
            // 验签没有抛出异常，说明验签成功
            log.info("退款通知验签成功");

            // 接受且验签成功:HTTP应答状态码需返回200或204
            response.setStatus(200);
            return R.ok().setMessage("退款通知验签成功");
        } catch (Exception e) {
            // 接受或验签失败:HTTP应答状态码需返回5XX或4XX，同时需返回应答报文
            log.info("退款通知验签失败");
            response.setStatus(500);
            return R.error().setMessage("退款通知验签失败");
        }
    }

    @ApiOperation("查询退款单")
    @GetMapping("/query-refund/{refundNo}")
    public R queryRefund(@PathVariable String refundNo) {
        try {
            String result = wxPayService.queryRefund(refundNo);
            return R.ok().setMessage("查询成功").data("result", result);
        } catch (Exception e) {
            return R.error().setMessage("查询失败").data("errorReason", e.getMessage());
        }
    }
}
