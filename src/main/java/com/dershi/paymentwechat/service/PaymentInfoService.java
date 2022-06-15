package com.dershi.paymentwechat.service;

import java.util.Map;

public interface PaymentInfoService {
    void createPaymentInfoForWx(String plainText);
    void createPaymentInfoForAli(Map<String, String> params);
}
