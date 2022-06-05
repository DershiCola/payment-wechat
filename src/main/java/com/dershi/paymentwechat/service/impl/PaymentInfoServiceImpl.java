package com.dershi.paymentwechat.service.impl;

import com.dershi.paymentwechat.entity.PaymentInfo;
import com.dershi.paymentwechat.mapper.PaymentInfoMapper;
import com.dershi.paymentwechat.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
