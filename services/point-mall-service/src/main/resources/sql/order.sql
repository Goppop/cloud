CREATE TABLE `order` (
                         id              VARCHAR(50) PRIMARY KEY COMMENT '订单ID',
                         user_id         VARCHAR(50) NOT NULL COMMENT '用户ID',
                         product_id      VARCHAR(50) NOT NULL COMMENT '商品ID，关联 product 表',
                         type            ENUM('API', 'LOCAL') NOT NULL COMMENT '商品类型（API: 供应商商品, LOCAL: 本地商品）',
                         external_order_id VARCHAR(50) COMMENT '供应商 API 订单ID（API 商品适用，本地商品为空）',
                         inventory_id    VARCHAR(50) COMMENT '本地库存ID（本地商品适用，API 商品为空）',
                         status         ENUM('PENDING', 'COMPLETED', 'CANCELED') DEFAULT 'PENDING' COMMENT '订单状态',
                         created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
                         FOREIGN KEY (inventory_id) REFERENCES inventory(id) ON DELETE SET NULL
);
