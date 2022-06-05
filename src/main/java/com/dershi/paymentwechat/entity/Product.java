package com.dershi.paymentwechat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_product")
public class Product extends BaseEntity{

    private String title; //商品名称

    private Integer price; //价格（分）
}
