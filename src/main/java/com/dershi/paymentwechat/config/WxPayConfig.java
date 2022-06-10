package com.dershi.paymentwechat.config;

import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

@Configuration
@ConfigurationProperties(prefix = "wxpay")
@PropertySource("classpath:wxpay.properties")
@Data
@Slf4j
public class WxPayConfig {
    // 商户号
    private String mchId;

    // 商户API证书序列号
    private String mchSerialNo;

    // 商户私钥文件
    private String privateKeyPath;

    // APIv3密钥
    private String apiV3Key;

    // APPID
    private String appid;

    // 微信服务器地址
    private String domain;

    // 接收结果通知地址
    private String notifyDomain;

    /**
     * 加载商户私钥
     * @param filename:私钥文件所在地址
     * @return 商户私钥
     */
    private PrivateKey getPrivateKey(String filename) {
        try {
            return PemUtil.loadPrivateKey(
                    new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("私钥不存在", e);
        }
    }

    /**
     * 获取签名验证器
     * @return Verifier
     */
    @Bean
    public Verifier getVerifier() {
        log.info("获取签名验证器");
        // 商户私钥对象
        PrivateKey privateKey = getPrivateKey(privateKeyPath);
        // 私钥签名对象
        PrivateKeySigner privateKeySigner = new PrivateKeySigner(mchSerialNo, privateKey);
        // 身份认证对象
        WechatPay2Credentials wechatPay2Credentials = new WechatPay2Credentials(mchId, privateKeySigner);

        // 获取证书管理器实例
        CertificatesManager certificatesManager;
        // 签名验证器对象
        Verifier verifier;
        try {
            certificatesManager = CertificatesManager.getInstance();
            // 向证书管理器增加需要自动更新平台证书的商户信息
            certificatesManager.putMerchant(mchId, wechatPay2Credentials, apiV3Key.getBytes(StandardCharsets.UTF_8));
            // ... 若有多个商户号，可继续调用putMerchant添加商户信息

            // 从证书管理器中获取verifier
            verifier = certificatesManager.getVerifier(mchId);
            return verifier;
        } catch (Exception e) {
            throw new RuntimeException("获取签名验证器失败", e);
        }
    }

    /**
     * 获取http连接对象
     * @param verifier 签名验证器对象
     * @return CloseableHttpClient
     */
    @Bean(name = "wxPayClient")
    public CloseableHttpClient getWxPayClient(Verifier verifier) {
        log.info("获取需要应答的HttpClient对象 => wxPayClient");
        // 商户私钥对象
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(mchId, mchSerialNo, privateKey)
                .withValidator(new WechatPay2Validator(verifier));
        // ... 接下来，你仍然可以通过builder设置各种参数，来配置你的HttpClient

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        return builder.build();
        // 后面跟使用Apache HttpClient一样

    }

    /**
     * 获取http连接对象(无需应答)
     * @return CloseableHttpClient
     */
    @Bean(name = "wxPayNoSignClient")
    public CloseableHttpClient getWxPayNoSignClient() {
        log.info("获取无需应答的HttpClient对象 => wxPayNoSignClient");
        // 商户私钥对象
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(mchId, mchSerialNo, privateKey)
                .withValidator(response -> true);
        // ... 接下来，你仍然可以通过builder设置各种参数，来配置你的HttpClient

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        return builder.build();
    }
}
