package com.dershi.paymentwechat.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@MapperScan("com.dershi.paymentwechat.mapper")
public class MybatiPlusConfig {
}
