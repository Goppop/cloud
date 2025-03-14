package com.atguigu.order.controller;

import com.atguigu.order.bean.Order;
import com.atguigu.order.properties.OrderProperties;
import com.atguigu.order.service.impl.OrderServiceImpl;
import com.atguigu.product.bean.Product;
import jakarta.annotation.Resource;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class OrderController {
    @Resource
    OrderServiceImpl orderService;
    @Resource
    OrderProperties orderProperties;


    @GetMapping("config")
    public String config(){
        return "order.timeout="+orderProperties.getTimeout() +";"+"order.auto-firm" +orderProperties.getAutoConfirm()+"space"+orderProperties.getDbUrl();
    }


    @GetMapping("/create")
    public Order createOrder(@RequestParam("userId") long userId ,@RequestParam("productId") long productId){
        Order order = orderService.createOrder(productId,userId);

        return order;
    }

}
