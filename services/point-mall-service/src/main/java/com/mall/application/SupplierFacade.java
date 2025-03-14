package com.mall.application;

import com.mall.domain.model.SupplierRequest;
import com.mall.domain.model.SupplierResponse;
import com.mall.infrastructure.client.SupplierApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupplierFacade {
    @Autowired
    private SupplierApiClient supplierApiClient;

    public SupplierResponse createOrder(String supplierName, SupplierRequest request) {
        return supplierApiClient.createOrder(supplierName, request);
    }

    public SupplierResponse getOrderStatus(String supplierName, String orderId) {
        return supplierApiClient.getOrderStatus(supplierName, orderId);
    }

    public SupplierResponse cancelOrder(String supplierName, String orderId) {
        return supplierApiClient.cancelOrder(supplierName, orderId);
    }

    public SupplierResponse redeemCard(String supplierName, SupplierRequest request) {
        return supplierApiClient.redeemCard(supplierName, request);
    }
}
