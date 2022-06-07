package com.dershi.paymentwechat.service.impl;

import com.dershi.paymentwechat.config.WxPayConfig;
import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.enums.OrderStatus;
import com.dershi.paymentwechat.enums.wxpay.WxApiType;
import com.dershi.paymentwechat.enums.wxpay.WxNotifyType;
import com.dershi.paymentwechat.service.WxPayService;
import com.dershi.paymentwechat.util.OrderNoUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {
    @Resource
    WxPayConfig wxPayConfig;

    @Resource
    private CloseableHttpClient wxPayClient;

    /**
     * 生成订单信息，调用统一Native下单API
     * @param productId:商品ID
     * @return 订单号和二维码链接
     */
    @Override
    public Map<String, Object> nativePay(Long productId) throws Exception {
        log.info("生成订单");
        /*
        生成订单
         */
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTitle("test");
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(1);
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        //TODO:将订单信息写入数据库

        /*
        生成二维码链接
         */
        // 调用统一下单API
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        Gson gson = new Gson();
        Map<Object, Object> paramsMap = new HashMap<>();
        paramsMap.put("appid", wxPayConfig.getAppid()); //应用ID
        paramsMap.put("mchid", wxPayConfig.getMchId()); //直连商户号
        paramsMap.put("description", orderInfo.getTitle()); //商品描述
        paramsMap.put("out_trade_no", orderInfo.getOrderNo()); //商户订单号
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType())); //通知地址:微信平台通知商户并发送请求的地址
        Map<Object, Object> amountMap = new HashMap<>();
        amountMap.put("total", orderInfo.getTotalFee()); //总金额
        amountMap.put("currency", "CNY"); //货币类型,CNY:人民币
        paramsMap.put("amount", amountMap); //订单金额

        // 将请求参数转换成json
        String paramsJson = gson.toJson(paramsMap);
        log.info("请求参数：" + paramsJson);
        // 设置entity
        StringEntity entity = new StringEntity(paramsJson,"utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        /*
        完成签名并执行请求
         */
        CloseableHttpResponse response = wxPayClient.execute(httpPost);
        try {
            String bodyAsString = EntityUtils.toString(response.getEntity()); //响应体字符串
            int statusCode = response.getStatusLine().getStatusCode(); //响应状态码
            if (statusCode == 200) { //处理成功
                log.info("成功, 响应结果 = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功");
            } else {
                log.info("Native下单失败, 响应状态码 = " + statusCode+ ", 响应结果 = " + bodyAsString);
                throw new IOException("request failed");
            }
            // 解析响应体得到二维码链接
            HashMap<String, String> bodyAsMap = gson.fromJson(bodyAsString, HashMap.class);
            String codeUrl = bodyAsMap.get("code_url");


            /*
            返回订单和二维码信息
             */
            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;

        } finally {
            response.close();
        }
    }
}
