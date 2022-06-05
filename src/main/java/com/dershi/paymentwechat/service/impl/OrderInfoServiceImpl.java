package com.dershi.paymentwechat.service.impl;

import com.dershi.paymentwechat.entity.OrderInfo;
import com.dershi.paymentwechat.mapper.OrderInfoMapper;
import com.dershi.paymentwechat.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

}
