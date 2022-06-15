package com.dershi.paymentwechat.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.domain.*;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.dershi.paymentwechat.config.AlipayClientConfig;
import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.entity.RefundInfo;
import com.dershi.paymentwechat.enums.OrderStatus;
import com.dershi.paymentwechat.enums.PayType;
import com.dershi.paymentwechat.enums.alipay.AliRefundStatus;
import com.dershi.paymentwechat.enums.alipay.AliTradeStatus;
import com.dershi.paymentwechat.enums.wxpay.WxRefundStatus;
import com.dershi.paymentwechat.enums.wxpay.WxTradeState;
import com.dershi.paymentwechat.service.AliPayService;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.dershi.paymentwechat.service.PaymentInfoService;
import com.dershi.paymentwechat.service.RefundInfoService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class AliPayServiceImpl implements AliPayService {
    @Resource
    private AlipayClient alipayClient;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private AlipayClientConfig alipayClientConfig;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    private final ReentrantLock lock = new ReentrantLock();

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String tradeCreate(Long productId) throws Exception {
        log.info("生成订单");
        /*
        生成订单
         */
        OrderInfo orderInfo = orderInfoService.getOrderInfoByProductId(productId, PayType.ALIPAY.getType());
        // 因为支付宝平台返回的是一个html页面，不是和微信平台一样的二维码链接，所以可以不对未支付订单的二维码信息是否存在进行判断

        // Alipay支付请求对象
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        // 根据业务设置可选的公共请求参数
        request.setNotifyUrl(alipayClientConfig.getNotifyUrl()); //支付宝平台异步请求通知的地址
        request.setReturnUrl(alipayClientConfig.getReturnUrl()); //请求成功跳转的地址

        // 设置请求参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(orderInfo.getOrderNo());
        BigDecimal total = new BigDecimal(orderInfo.getTotalFee().toString()).divide(new BigDecimal("100"));
        model.setTotalAmount(total.toString());
        model.setSubject(orderInfo.getTitle());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        request.setBizModel(model);

        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);

        if(response.isSuccess()){
            log.info("支付宝下单成功, 响应结果 => " + response.getBody());
            return response.getBody();
        } else {
            log.error("支付宝下单失败, 响应状态码 = " + response.getCode() + ", 信息 = " + response.getMsg());
            throw new RuntimeException("支付宝下单失败, 响应状态码 = " + response.getCode() + ", 信息 = " + response.getMsg());
        }
    }

    @Override
    public String tradeNotify(Map<String, String> params) {
        String result = "failure";

        try {
            /*
            第一次验签
             */
            boolean signVerified = AlipaySignature.rsaCheckV1(params,
                    alipayClientConfig.getAlipayPublicKey(),
                    AlipayConstants.CHARSET_UTF8,
                    AlipayConstants.SIGN_TYPE_RSA2); //调用SDK验证签名

            if(!signVerified){
                log.error("支付宝异步通知验签失败！");
                return result;
            }
            log.info("支付宝异步通知验签成功！");

            /*
            二次验签，4个步骤有任何一个验证不通过，则表明本次通知是异常通知
             */
            // 1.商家需要验证该通知数据中的 out_trade_no 是否为商家系统中创建的订单号
            OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(params.get("out_trade_no"));
            if (orderInfo == null) {
                log.error("订单不存在！");
                return result;
            }

            // 2.判断 total_amount 是否确实为该订单的实际金额（即商家订单创建时的金额）
            int totalAmount = new BigDecimal(params.get("total_amount")).multiply(new BigDecimal("100")).intValue();
            if (totalAmount != orderInfo.getTotalFee()) {
                log.error("订单金额校验失败！");
                return result;
            }

            // 3.校验通知中的 seller_id（或者 seller_email) 是否为 out_trade_no 这笔单据的对应的操作方（有的时候，一个商家可能有多个 seller_id/seller_email）
            if (!params.get("seller_id").equals(alipayClientConfig.getSellerId())) {
                log.error("商户ID校验失败！");
                return result;
            }

            // 4.验证 app_id 是否为该商家本身
            if (!params.get("app_id").equals(alipayClientConfig.getAppId())) {
                log.error("APPID校验失败！");
                return result;
            }

            // 在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功
            if (!(AliTradeStatus.SUCCESS.getType().equals(params.get("trade_status")) || AliTradeStatus.FINISHED.getType().equals(params.get("trade_status")))) {
                log.error("支付未成功！");
                return result;
            }

            /*
            处理商户业务，更新订单信息
             */
            processOrder(params);

            // 向支付宝返回成功结果
            result = "success";

        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processOrder(Map<String, String> params) {
        log.info("处理支付宝支付订单");

        if (lock.tryLock()) {
            try {
                // 处理重复通知
                String orderStatus = orderInfoService.getOrderStatusByOrderNo(params.get("out_trade_no"));
                if (!OrderStatus.NOTPAY.getType().equals(orderStatus))
                    return;

                // 更新订单状态
                orderInfoService.updateOrderStatusByOrderNo(params.get("out_trade_no"), OrderStatus.SUCCESS.getType());
                log.info("更新订单状态 => {}", OrderStatus.SUCCESS.getType());

                // 记录支付日志
                paymentInfoService.createPaymentInfoForAli(params);
                log.info("记录支付日志");
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo) throws Exception {
        // 调用支付宝平台的关闭订单API
        closeOrder(orderNo);

        // 商户更新订单状态为"用户已取消"
        orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.CANCEL.getType());
    }

    private void closeOrder(String orderNo) throws Exception {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        model.setOutTradeNo(orderNo);
        request.setBizModel(model);
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            log.info("支付宝订单取消成功, 响应信息 => {}", response.getBody());
        } else {
            log.error("支付宝订单取消失败, 响应码 => " + response.getCode() + ", 响应信息 => " + response.getMsg());
//            throw new RuntimeException(response.getMsg());
        }
    }

    @Override
    public String queryOrder(String orderNo) throws Exception {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(orderNo);
        request.setBizModel(model);
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            log.info("支付宝订单查询成功, 响应信息 => {}", response.getBody());
            return response.getBody();
        } else {
            log.error("支付宝订单查询失败, 响应码 => " + response.getCode() + ", 响应信息 => " + response.getMsg());
//            throw new RuntimeException(response.getMsg());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkOrderStatus(String orderNo) throws Exception {
        Gson gson = new Gson();
        String bodyJson = queryOrder(orderNo);
        // 支付宝平台未创建订单
        if (bodyJson == null) {
            log.info("核实订单未在支付宝平台创建 => {}", orderNo);
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.CLOSED.getType());
            return;
        }
        HashMap<String, String> map = gson.fromJson(bodyJson, HashMap.class);

        // 确认订单支付成功
        if (AliTradeStatus.SUCCESS.getType().equals(map.get("trade_status")) || AliTradeStatus.FINISHED.getType().equals(map.get("trade_status"))) {
            log.info("支付宝平台确认订单状态为\"支付成功\" => {}", orderNo);
            // 更新商户订单的订单状态为"支付成功"
            log.info("更新商户订单状态为\"支付成功\" => {}", orderNo);
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.SUCCESS.getType());
            // 记录支付日志
            log.info("创建支付日志 => {}", orderNo);
            paymentInfoService.createPaymentInfoForAli(map);
        }

        // 确认订单未支付
        if (AliTradeStatus.NOTPAY.getType().equals(map.get("trade_status"))) {
            log.info("支付宝平台确认订单状态为\"未支付\" => {}", orderNo);
            // 关闭订单
            log.info("关闭订单 => {}", orderNo);
            closeOrder(orderNo);
            // 更新商户订单的订单状态为"超时已关闭"
            log.info("更新商户订单状态为\"超时已关闭\" => {}", orderNo);
            orderInfoService.updateOrderStatusByOrderNo(orderNo, OrderStatus.CLOSED.getType());
        }
    }

    @Override
    public void refunds(String orderNo, String reason) throws Exception {
        // 创建退款订单记录对象
        RefundInfo refundInfo = refundInfoService.createRefundInfoByOrderNo(orderNo, reason);
        log.info("创建退款订单 = {}", refundInfo.getRefundNo());

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(orderNo);
        model.setRefundAmount(new BigDecimal(refundInfo.getRefund().toString()).divide(new BigDecimal("100")).toString());
        request.setBizModel(model);
        AlipayTradeRefundResponse response = alipayClient.execute(request);

        if(response.isSuccess()){
            log.info("支付宝退款成功, 响应结果 => " + response.getBody());

            // 更新商户订单
            orderInfoService.updateOrderStatusByOrderNo(refundInfo.getOrderNo(), OrderStatus.REFUND_SUCCESS.getType());

            // 更新退款单信息
            refundInfoService.updateRefundInfoForAli(refundInfo.getRefundNo(), response.getBody(), AliRefundStatus.REFUND_SUCCESS.getType());

        } else {
            log.error("支付宝退款失败, 响应状态码 = " + response.getCode() + ", 信息 = " + response.getMsg());
            // 更新商户订单
            orderInfoService.updateOrderStatusByOrderNo(refundInfo.getOrderNo(), OrderStatus.REFUND_ABNORMAL.getType());

            // 更新退款单信息
            refundInfoService.updateRefundInfoForAli(refundInfo.getRefundNo(), response.getBody(), AliRefundStatus.REFUND_ERROR.getType());

            throw new RuntimeException("支付宝退款失败, 响应状态码 = " + response.getCode() + ", 信息 = " + response.getMsg());
        }
    }

    @Override
    public String queryRefund(String orderNo) throws Exception {
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
        model.setOutTradeNo(orderNo);
        model.setOutRequestNo(orderNo);
        request.setBizModel(model);
        AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            log.info("查询退款订单成功, 响应信息 => {}", response.getBody());
            return response.getBody();
        } else {
            log.error("查询退款订单失败, 响应状态码 = " + response.getCode() + ", 信息 = " + response.getMsg());
            throw new RuntimeException("查询退款订单失败, 响应状态码 = " + response.getCode() + ", 信息 = " + response.getMsg());
        }
    }

    @Override
    public String downloadBill(String billDate, String billType) throws Exception {
        AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
        AlipayDataDataserviceBillDownloadurlQueryModel model = new AlipayDataDataserviceBillDownloadurlQueryModel();
        model.setBillDate(billDate);
        model.setBillType(billType);
        request.setBizModel(model);
        AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            log.info("获取账单下载链接成功, 响应信息 => {}", response.getBody());
            Gson gson = new Gson();
            HashMap<String, Object> map = gson.fromJson(response.getBody(), HashMap.class);
            Map<String, String> resMap = (Map<String, String>) map.get("alipay_data_dataservice_bill_downloadurl_query_response");
            return resMap.get("bill_download_url");
        } else {
            log.error("获取账单下载链接失败, 响应状态码 = " + response.getCode() + ", 信息 = " + response.getMsg());
            throw new RuntimeException("获取账单下载链接失败, 响应状态码 = " + response.getCode() + ", 信息 = " + response.getMsg());
        }
    }
}
