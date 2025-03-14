package com.atguigu.product.service;

import com.atguigu.product.bean.Product;

public interface ProductService {
    Product GetProductById(long productId);

    Product getProductByIdFromRedis(long productId);
}
