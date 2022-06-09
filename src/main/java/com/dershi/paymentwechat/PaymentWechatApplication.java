package com.dershi.paymentwechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 打开定时操作
public class PaymentWechatApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentWechatApplication.class, args);
    }

}
