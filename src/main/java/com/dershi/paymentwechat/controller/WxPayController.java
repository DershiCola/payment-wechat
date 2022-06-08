package com.dershi.paymentwechat.controller;

import com.dershi.paymentwechat.config.WxPayConfig;
import com.dershi.paymentwechat.service.WxPayService;
import com.dershi.paymentwechat.util.HttpUtils;
import com.dershi.paymentwechat.vo.R;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.notification.Notification;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationHandler;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


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
    public R nativePay(@PathVariable Long productId) throws Exception {
        log.info("发起支付请求");
        Map<String, Object> map = wxPayService.nativePay(productId);
        return R.ok().setData(map);
    }

    @ApiOperation("支付通知")
    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response) {
        Gson gson = new Gson();

        // 响应报文
        Map<String, String> map = new HashMap<>();

        try {
            Notification notification = wxPayService.nativeNotify(request);
            // 验签没有抛出异常，说明验签成功
            log.info("通知验签成功");

            // 处理更新订单
            wxPayService.processorOrder(notification);

            // 接受且验签成功:HTTP应答状态码需返回200或204
            response.setStatus(200);
            map.put("code", "success");
            map.put("message", "通知验签成功");
            return gson.toJson(map);
        } catch (Exception e) {
            // 接受或验签失败:HTTP应答状态码需返回5XX或4XX，同时需返回应答报文
            log.info("通知验签失败");
            response.setStatus(500);
            map.put("code", "fail");
            map.put("message", e.getMessage());
            return gson.toJson(map);
        }

    }
}
