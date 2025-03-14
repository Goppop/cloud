package com.atguigu.product.service.impl;

import com.atguigu.product.bean.Product;
import com.atguigu.product.mapper.ProductMapper;
import com.atguigu.product.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ProductMapper productMapper; // 注入 Mapper


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Product GetProductById(long productId) {
        Product product =new Product();
        product.setId(productId);
        product.setPrice(new BigDecimal("99"));
        product.setProductName("手机"+productId);
        product.setNum(2);
        return product;
    }

    @Override
    public Product getProductByIdFromRedis(long productId) {
        String redisKey = "product:" + productId;

        // 先从缓存中取
        String cachedProduct = redisTemplate.opsForValue().get(redisKey);
        if (cachedProduct != null) {
            return deserializeProduct(cachedProduct); // 缓存中存在数据，直接返回
        }

        // 缓存中不存在，查询数据库
        Product product = queryProductFromDb(productId);

        // 将数据存入缓存，并设置过期时间（例如 1小时）
        redisTemplate.opsForValue().set(redisKey, serializeProduct(product), 1, TimeUnit.HOURS);

        return product;
    }

    private Product queryProductFromDb(long productId) {
        // 模拟从数据库查询商品
        return productMapper.findById(productId);

    }

    private String serializeProduct(Product product) {
        try {
            return objectMapper.writeValueAsString(product); // 使用 Jackson 序列化
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 Product 失败", e);
        }
    }

    private Product deserializeProduct(String data) {
        try {
            return objectMapper.readValue(data, Product.class); // 反序列化 JSON
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化 Product 失败", e);
        }
    }
}
