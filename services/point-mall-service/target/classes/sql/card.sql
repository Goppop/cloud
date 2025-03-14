CREATE TABLE card (
                      id          VARCHAR(50) PRIMARY KEY COMMENT '卡密ID',
                      product_id  VARCHAR(50) NOT NULL COMMENT '商品ID，关联 product 表',
                      code        VARCHAR(100) NOT NULL COMMENT '卡密兑换码',
                      status      ENUM('AVAILABLE', 'USED') DEFAULT 'AVAILABLE' COMMENT '卡密状态',
                      user_id     VARCHAR(50) COMMENT '兑换用户ID（如果已兑换）',
                      order_id    VARCHAR(50) COMMENT '订单ID（如果已兑换）',
                      created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);
