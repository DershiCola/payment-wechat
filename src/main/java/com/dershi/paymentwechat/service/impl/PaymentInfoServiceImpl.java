package com.dershi.paymentwechat.service.impl;

import com.dershi.paymentwechat.entity.PaymentInfo;
import com.dershi.paymentwechat.enums.PayType;
import com.dershi.paymentwechat.mapper.PaymentInfoMapper;
import com.dershi.paymentwechat.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Override
    public void createPaymentInfo(String plainText) {
        Gson gson = new Gson();
        HashMap<String, Object> map = gson.fromJson(plainText, HashMap.class);

        // 商户订单号
        String orderNo = (String) map.get("out_trade_no");
        // 微信支付编号
        String transactionId = (String) map.get("transaction_id");
        // 交易类型
        String tradeType = (String) map.get("trade_type");
        // 交易状态
        String tradeState = (String) map.get("trade_state");
        // 用户支付金额
        Map<String, Object> amount = (Map<String, Object>) map.get("amount");
        Integer payerTotal = ((Double) amount.get("payer_total")).intValue();

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(payerTotal);
        paymentInfo.setContent(plainText);

        baseMapper.insert(paymentInfo);
    }
}
