package com.mall.api;

import com.mall.application.SupplierFacade;
import com.mall.domain.model.SupplierRequest;
import com.mall.domain.model.SupplierResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplier")
public class SupplierController {

    @Autowired
    private SupplierFacade supplierFacade;  // ✅ 这里必须保证 `SupplierFacade` 存在并正确注入

    // 统一下单
    @PostMapping("/{supplierName}/order")
    public ResponseEntity<SupplierResponse> createOrder(
            @PathVariable String supplierName,
            @RequestBody SupplierRequest request) {
        SupplierResponse response = supplierFacade.createOrder(supplierName, request);
        return ResponseEntity.ok(response);
    }

    // 查询订单状态
    @GetMapping("/{supplierName}/order/{orderId}")
    public ResponseEntity<SupplierResponse> getOrderStatus(
            @PathVariable String supplierName,
            @PathVariable String orderId) {
        SupplierResponse response = supplierFacade.getOrderStatus(supplierName, orderId);
        return ResponseEntity.ok(response);
    }

    // 取消订单
    @PostMapping("/{supplierName}/order/{orderId}/cancel")
    public ResponseEntity<SupplierResponse> cancelOrder(
            @PathVariable String supplierName,
            @PathVariable String orderId) {
        SupplierResponse response = supplierFacade.cancelOrder(supplierName, orderId);
        return ResponseEntity.ok(response);
    }

    // 兑换卡密
    @PostMapping("/{supplierName}/redeem")
    public ResponseEntity<SupplierResponse> redeemCard(
            @PathVariable String supplierName,
            @RequestBody SupplierRequest request) {
        SupplierResponse response = supplierFacade.redeemCard(supplierName, request);
        return ResponseEntity.ok(response);
    }
}

