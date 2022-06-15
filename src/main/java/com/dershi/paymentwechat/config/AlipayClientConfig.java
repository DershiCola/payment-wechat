package com.dershi.paymentwechat.config;

import com.alipay.api.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "alipay")
@PropertySource("classpath:alipay-sandbox.properties")
@Data
public class AlipayClientConfig {

    private String appId;

    private String sellerId;

    private String merchantPrivateKey;

    private String gatewayUrl;

    private String alipayPublicKey;

    private String contentKey;

    private String returnUrl;

    private String notifyUrl;

    @Bean
    public AlipayClient getAliPayClient() throws AlipayApiException {
        AlipayConfig alipayConfig = new AlipayConfig();
        /*
        必要的公共请求参数
         */
        //设置网关地址
        alipayConfig.setServerUrl(gatewayUrl);
        //设置应用ID
        alipayConfig.setAppId(appId);
        //设置应用私钥
        alipayConfig.setPrivateKey(merchantPrivateKey);
        //设置请求格式，固定值json
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        //设置字符集
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        //设置支付宝公钥
        alipayConfig.setAlipayPublicKey(alipayPublicKey);
        //设置签名类型
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);

        return new DefaultAlipayClient(alipayConfig);
    }
}
