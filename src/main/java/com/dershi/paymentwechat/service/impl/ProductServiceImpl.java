package com.dershi.paymentwechat.service.impl;

import com.dershi.paymentwechat.entity.Product;
import com.dershi.paymentwechat.mapper.ProductMapper;
import com.dershi.paymentwechat.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
