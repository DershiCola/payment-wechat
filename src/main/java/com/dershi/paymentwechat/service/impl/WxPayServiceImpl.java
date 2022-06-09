package com.dershi.paymentwechat.service.impl;

import com.dershi.paymentwechat.config.WxPayConfig;
import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.enums.OrderStatus;
import com.dershi.paymentwechat.enums.wxpay.WxApiType;
import com.dershi.paymentwechat.enums.wxpay.WxNotifyType;
import com.dershi.paymentwechat.enums.wxpay.WxTradeState;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.dershi.paymentwechat.service.PaymentInfoService;
import com.dershi.paymentwechat.service.WxPayService;
import com.dershi.paymentwechat.util.HttpUtils;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.notification.Notification;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationHandler;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.wechat.pay.contrib.apache.httpclient.constant.WechatPayHttpHeaders.*;
import static com.wechat.pay.contrib.apache.httpclient.constant.WechatPayHttpHeaders.WECHAT_PAY_SIGNATURE;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {
    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private Verifier verifier;

    @Resource
    private CloseableHttpClient wxPayClient;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    private final ReentrantLock lock = new ReentrantLock();

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
        OrderInfo orderInfo = orderInfoService.getOrderInfoByProductId(productId);
        String codeUrl = orderInfo.getCodeUrl();
        // 已存在未支付的订单且有二维码
        if (!StringUtils.isEmpty(codeUrl)) {
            log.info("存在未支付订单和对应二维码，且在2小时有效期内");
            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;
        }

        /*
        生成二维码链接
         */
        // 调用统一下单API
        // https://api.mch.weixin.qq.com/v3/pay/transactions/native
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        Gson gson = new Gson();
        Map<Object, Object> paramsMap = new HashMap<>();
        // 设置请求参数
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
        try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
            String bodyAsString = EntityUtils.toString(response.getEntity()); //响应体字符串
            int statusCode = response.getStatusLine().getStatusCode(); //响应状态码
            if (statusCode == 200) { //处理成功
                log.info("成功, 响应结果 = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("成功");
            } else {
                log.info("Native下单失败, 响应状态码 = " + statusCode + ", 响应结果 = " + bodyAsString);
                throw new IOException("request failed");
            }
            // 解析响应体得到二维码链接
            HashMap<String, String> bodyAsMap = gson.fromJson(bodyAsString, HashMap.class);
            codeUrl = bodyAsMap.get("code_url");

            // 保存二维码链接
            log.info("保存了新的二维码链接");
            orderInfoService.saveCodeUrl(orderInfo.getOrderNo(), codeUrl);

            /*
            返回订单和二维码信息
             */
            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;
        }
    }

    /**
     * 回调通知，微信支付平台向商户发送请求，通知商户订单的支付结果
     * @param request:微信支付平台的http请求
     * @throws Exception:验签失败告知上层调用
     */
    @Override
    public void nativeNotify(HttpServletRequest request) throws Exception {
        Gson gson = new Gson();

        // 处理通知请求
        String body = HttpUtils.readData(request);
        HashMap<String, Object> bodyMap = gson.fromJson(body, HashMap.class);

        log.info("通知ID => {}", bodyMap.get("id"));
        log.info("通知信息整体 => {}", body);

        // 根据SDK封装通知请求
        NotificationRequest notificationRequest = new NotificationRequest.Builder().withSerialNumber(request.getHeader(WECHAT_PAY_SERIAL))
                .withNonce(request.getHeader(WECHAT_PAY_NONCE))
                .withTimestamp(request.getHeader(WECHAT_PAY_TIMESTAMP))
                .withSignature(request.getHeader(WECHAT_PAY_SIGNATURE))
                .withBody(body)
                .build();
        NotificationHandler handler = new NotificationHandler(verifier, wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));

        /*
        验签和解析通知参数
         */
        // 验签成功得到通知对象Notification
        Notification notification = handler.parse(notificationRequest);

        // 机密通知信息，更新订单，记录支付日志
        processorOrder(notification);

    }

    /**
     * 解密被加密的通知信息，获取订单支付成功的具体信息，并更新处理订单，记录支付日志
     * @param notification:验签成功后得到的通知对象
     */
    private void processorOrder(Notification notification) {
        Gson gson = new Gson();

        // 解密 -> 获取支付成功的通知信息参数
        String plainText = notification.getDecryptData();
        log.info("解密得到通知明文 => {}", plainText);
        HashMap<String, String> map = gson.fromJson(plainText, HashMap.class);

        /*
        处理重复通知:根据订单状态来判断是否已经通知并更新了数据，如果已经通知则直接返回，没通知则进行数据更新和日志记录
         */
        try {
            // 在对业务数据进行状态检查和处理之前，要采用数据锁进行并发控制，以避免函数重入造成的数据混乱
            if (lock.tryLock()) { //获取锁:成功获取返回true,获取失败返回false;拿不到锁可以不用等待
                String orderStatus = orderInfoService.getOrderStatusByOrderNo(map.get("out_trade_no"));
                // 通知的订单状态与数据库查询到的订单状态一致，则直接返回
                if (OrderStatus.valueOf(map.get("trade_state")).getType().equals(orderStatus)) {
                    return;
                }

                // 模拟并发问题
//                try {
//                    TimeUnit.SECONDS.sleep(5);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                //更新订单状态
                orderInfoService.updateOrderStatusByOrderNo(map.get("out_trade_no"), OrderStatus.valueOf(map.get("trade_state")).getType());
                log.info("更新订单状态 => {}", map.get("trade_state"));

                //记录支付日志
                paymentInfoService.createPaymentInfo(plainText);
            }
        } finally {
            // 主动释放锁
            lock.unlock();
        }
    }

    /**
     * 取消订单
     * @param orderNo:订单号
     * @throws Exception:关单失败告知上层调用
     */
    @Override
    public void cancelOrder(String orderNo) throws Exception {
        // 调用微信平台的关闭订单API
        closeOrder(orderNo);

        // 更新订单状态为"用户已取消"
        orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.CANCEL.getType());
    }

    private void closeOrder(String orderNo) throws Exception {
        // 调用关闭订单API
        // https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/{out_trade_no}/close
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo)));
        Gson gson = new Gson();
        Map<Object, Object> paramsMap = new HashMap<>();
        // 设置请求参数
        paramsMap.put("mchid", wxPayConfig.getMchId());
        String paramsJson = gson.toJson(paramsMap);
        // 设置entity
        StringEntity entity = new StringEntity(paramsJson,"utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        // 发起请求，得到响应(只有响应状态码，没有响应body)
        try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode(); //响应状态码
            if (statusCode == 200) { //成功取消订单
                log.info("成功取消订单");
            } else if (statusCode == 204) {
                log.info("成功取消订单");
            } else {
                log.info("取消下单失败, 响应状态码 = " + statusCode);
                throw new IOException("cancel order failed");
            }
        }
    }

    /**
     * 调用微信平台的查询订单API获取订单信息
     * @param orderNo:订单号
     * @return 调用API后返回的订单信息
     */
    @Override
    public String queryOrder(String orderNo) throws Exception {
        // 调用查单API
        // https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/{out_trade_no}
        HttpGet httpGet = new HttpGet(wxPayConfig.getDomain()
                .concat(String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo))
                .concat("?mchid=").concat(wxPayConfig.getMchId()));
        httpGet.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = wxPayClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 204) { //查询成功
                log.info("查询订单成功");
            } else {
                log.info("查询订单失败, 响应状态码 = " + statusCode + ", 错误信息 = " + EntityUtils.toString(response.getEntity()));
            }
            return EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            throw new Exception(EntityUtils.toString(wxPayClient.execute(httpGet).getEntity()));
        }
    }

    @Override
    public void checkOrderStatus(String orderNo) throws Exception {
        Gson gson = new Gson();
        String bodyJson = queryOrder(orderNo);
        HashMap<String, String> map = gson.fromJson(bodyJson, HashMap.class);

        // 确认订单支付成功
        if (WxTradeState.SUCCESS.getType().equals(map.get("trade_state"))) {
            log.info("微信平台确认订单状态为\"支付成功\" => {}", orderNo);
            // 更新商户订单的订单状态为"支付成功"
            log.info("更新商户订单状态为\"支付成功\" => {}", orderNo);
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.SUCCESS.getType());
            // 记录支付日志
            log.info("创建支付日志 => {}", orderNo);
            paymentInfoService.createPaymentInfo(bodyJson);
        }

        // 确认订单未支付
        if (WxTradeState.NOTPAY.getType().equals(map.get("trade_state"))) {
            log.info("微信平台确认订单状态为\"未支付\" => {}", orderNo);
            // 关闭订单
            log.info("关闭订单 => {}", orderNo);
            closeOrder(orderNo);
            // 更新商户订单的订单状态为"超时已关闭"
            log.info("更新商户订单状态为\"超时已关闭\" => {}", orderNo);
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.CLOSED.getType());
        }
    }
}
