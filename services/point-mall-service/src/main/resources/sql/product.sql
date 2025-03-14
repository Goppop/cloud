CREATE TABLE product (
                         id              VARCHAR(50) PRIMARY KEY COMMENT '商品ID',
                         name            VARCHAR(255) NOT NULL COMMENT '商品名称',
                         type            ENUM('API', 'LOCAL') NOT NULL COMMENT '商品类型（API: 供应商商品, LOCAL: 本地商品）',
                         supplier_name   VARCHAR(50) COMMENT '供应商名称（API 商品必填, 本地商品为空）',
                         price           INT NOT NULL COMMENT '商品兑换所需积分',
                         description     TEXT COMMENT '商品描述',
                         created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);
