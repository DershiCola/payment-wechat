package com.dershi.paymentwechat;

import com.dershi.paymentwechat.config.AlipayClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

@SpringBootTest
public class AlipayTest {
    @Resource
    private AlipayClientConfig config;

    @Test
    public void test() {
        System.out.println(config.getReturnUrl());
    }
}
