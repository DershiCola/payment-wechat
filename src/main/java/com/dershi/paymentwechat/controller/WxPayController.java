package com.dershi.paymentwechat.controller;

import com.dershi.paymentwechat.service.WxPayService;
import com.dershi.paymentwechat.vo.R;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网页微信支付API")
@Slf4j
public class WxPayController {
    @Resource
    WxPayService wxPayService;

    @PostMapping("/native/{productId}")
    public R nativePay(@PathVariable Long productId) throws Exception {
        log.info("发起支付请求");
        Map<String, Object> map = wxPayService.nativePay(productId);
        return R.ok().setData(map);
    }
}
