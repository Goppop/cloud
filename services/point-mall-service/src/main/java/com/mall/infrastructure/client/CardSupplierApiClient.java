package com.mall.infrastructure.client;

import com.mall.domain.model.SupplierRequest;
import com.mall.domain.model.SupplierResponse;
import org.springframework.stereotype.Service;

@Service
public class CardSupplierApiClient implements SupplierService{
    @Override
    public String getSupplierName() {
        return "KM";
    }

    @Override
    public SupplierResponse createOrder(SupplierRequest request) {
        return null;
    }

    @Override
    public SupplierResponse getOrderStatus(String orderId) {
        return null;
    }

    @Override
    public SupplierResponse cancelOrder(String orderId) {
        return null;
    }

    @Override
    public SupplierResponse redeemCard(SupplierRequest request) {
        return null;
    }
}
