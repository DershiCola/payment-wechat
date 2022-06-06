package com.dershi.paymentwechat;

import com.dershi.paymentwechat.config.WxPayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.security.PrivateKey;

@SpringBootTest
class PaymentWechatApplicationTests {
    @Resource
    private WxPayConfig wxPayConfig;


//    @Test
//    void testGetPrivateKey() {
//        System.out.println(wxPayConfig.getPrivateKey(wxPayConfig.getPrivateKeyPath()));
//    }

}
