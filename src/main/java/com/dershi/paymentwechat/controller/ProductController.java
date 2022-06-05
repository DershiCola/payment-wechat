package com.dershi.paymentwechat.controller;

import com.dershi.paymentwechat.service.ProductService;
import com.dershi.paymentwechat.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;

@CrossOrigin  // 开启前端跨域
@Api(tags = "商品管理")
@RestController
@RequestMapping("/api/product")
public class ProductController {
    @Resource
    private ProductService productService;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public R test() {
        return R.ok().data("now", new Date());
    }

    @GetMapping("list")
    public R list() {
        return R.ok().data("productList", productService.list());
    }
}
