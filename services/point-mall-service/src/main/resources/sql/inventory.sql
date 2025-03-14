CREATE TABLE inventory (
                           id          VARCHAR(50) PRIMARY KEY COMMENT '库存ID',
                           product_id  VARCHAR(50) NOT NULL COMMENT '商品ID，关联 product 表',
                           status      ENUM('AVAILABLE', 'USED') DEFAULT 'AVAILABLE' COMMENT '库存状态（可用/已兑换）',
                           serial_number VARCHAR(100) COMMENT '商品序列号（如实物 SN 码，或体验卡号）',
                           created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);
