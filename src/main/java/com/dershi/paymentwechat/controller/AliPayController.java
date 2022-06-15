package com.dershi.paymentwechat.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayConstants;
import com.alipay.api.internal.util.AlipaySignature;
import com.dershi.paymentwechat.config.AlipayClientConfig;
import com.dershi.paymentwechat.service.AliPayService;
import com.dershi.paymentwechat.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/ali-pay")
@Api(tags = "网页支付宝支付API")
@Slf4j
public class AliPayController {
    @Resource
    private AliPayService aliPayService;

    @Resource
    private AlipayClientConfig alipayClientConfig;

    @ApiOperation("统一收单下单并支付页面接口")
    @PostMapping("/trade/page/pay/{productId}")
    public R tradePagePay(@PathVariable Long productId) {
        log.info("调用支付宝统一收单下单并支付页面接口");

        // 支付宝开发平台接受 request 对象后
        // 会为开发者返回一个 html 形式的 form 表单，包含自动提交的脚本
        String formStr;
        try {
            formStr = aliPayService.tradeCreate(productId);
            // 将 form 表单字符串返回给前端，前端调用自动提交脚本，进行表单提交
            // 表单会提交到 action 属性所指向的支付宝开发平台中，从而展示一个支付页面
            return R.ok().data("formStr", formStr);
        } catch (Exception e) {
            return R.error().data("errorMsg", e.getMessage());
        }
    }

    @ApiOperation("支付宝异步通知")
    @PostMapping("/trade/notify")
    public String tradeNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝异步通知");
        log.info("通知参数 => {}", params);

        // 接收通知成功必须返回"success"，否则支付宝平台将重复通知
        return aliPayService.tradeNotify(params);
    }

    @ApiOperation("取消订单")
    @PostMapping("/trade/close/{orderNo}")
    public R cancelOrder(@PathVariable String orderNo) {
        log.info("取消订单 = {}", orderNo);
        try {
            aliPayService.cancelOrder(orderNo);
            return R.ok().setMessage("成功取消订单");
        } catch (Exception e) {
            return R.error().setMessage("取消订单失败").data("errorReason", e.getMessage());
        }
    }

    @ApiOperation("查询订单-测试用")
    @GetMapping("/trade/query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) {
        log.info("调用查单接口 => {}", orderNo);
        try {
            String result = aliPayService.queryOrder(orderNo);
            return R.ok().setMessage("查询成功").data("result", result);
        } catch (Exception e) {
            return R.error().setMessage("查询失败").data("errorReason", e.getMessage());
        }
    }

    @ApiOperation("订单退款")
    @PostMapping("/trade/refund/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) {
        try {
            aliPayService.refunds(orderNo, reason);
            return R.ok().setMessage("退款成功");
        } catch (Exception e) {
            return R.error().setMessage("退款失败").data("errorReason", e.getMessage());
        }
    }

    @ApiOperation("查询退款订单-测试用")
    @GetMapping("/trade/fastpay/refund/query/{orderNo}")
    public R queryRefund(@PathVariable String orderNo) {
        log.info("调用查询退单接口 => {}", orderNo);
        try {
            String result = aliPayService.queryRefund(orderNo);
            return R.ok().setMessage("查询退款订单成功").data("result", result);
        } catch (Exception e) {
            return R.error().setMessage("查询退款订单失败").data("errorReason", e.getMessage());
        }
    }

    @ApiOperation("获取账单url")
    @GetMapping("/bill/downloadurl/query/{billDate}/{billType}")
    public R downloadBill(@PathVariable String billDate, @PathVariable String billType) {
        try {
            log.info("下载账单...");
            String downloadUrl = aliPayService.downloadBill(billDate, billType);
            log.info("账单下载链接 => {}", downloadUrl);
            return R.ok().setMessage("成功下载账单").data("downloadUrl", downloadUrl);
        } catch (Exception e) {
            return R.error().setMessage("下载账单失败");
        }
    }

}
