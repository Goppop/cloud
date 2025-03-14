package com.mall.infrastructure.client;

import com.mall.domain.model.SupplierRequest;
import com.mall.domain.model.SupplierResponse;

/**
 * 供应商 API 统一接口
 */
public interface SupplierService {
    /**
     * 获取供应商名称（用于匹配供应商）
     */
    String getSupplierName();

    /**
     * 创建订单（适用于 JD 购物、好医生预约等）
     */
    SupplierResponse createOrder(SupplierRequest request);

    /**
     * 查询订单状态
     */
    SupplierResponse getOrderStatus(String orderId);

    /**
     * 取消订单
     */
    SupplierResponse cancelOrder(String orderId);

    /**
     * 兑换卡密（适用于卡密兑换供应商）
     */
    SupplierResponse redeemCard(SupplierRequest request);
}
