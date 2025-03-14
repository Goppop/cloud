package com.atguigu.product.controller;
import com.atguigu.product.bean.Product;
import com.atguigu.product.service.impl.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {
    @Autowired
    ProductServiceImpl productService;

    @GetMapping("/product/{id}")
    public Product getProduct(@PathVariable("id") long productId){


        Product product2 = productService.getProductByIdFromRedis(productId);
        return  product2;

    }
}
