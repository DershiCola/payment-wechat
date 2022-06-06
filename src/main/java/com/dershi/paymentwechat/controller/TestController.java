package com.dershi.paymentwechat.controller;

import com.dershi.paymentwechat.config.WxPayConfig;
import com.dershi.paymentwechat.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "测试控制器")
@RestController
@RequestMapping("/api/test")
public class TestController {
    @Resource
    private WxPayConfig wxPayConfig;

    @GetMapping
    public R test() {
        return R.ok().data("mchId", wxPayConfig.getMchId());
    }
}
