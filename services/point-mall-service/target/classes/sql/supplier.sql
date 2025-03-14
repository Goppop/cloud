CREATE TABLE supplier (
                          id          VARCHAR(50) PRIMARY KEY COMMENT '供应商ID',
                          name        VARCHAR(100) NOT NULL COMMENT '供应商名称',
                          api_url     VARCHAR(255) COMMENT 'API 地址',
                          api_key     VARCHAR(100) COMMENT 'API Key',
                          created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);
