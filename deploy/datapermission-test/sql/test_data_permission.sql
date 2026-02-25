-- ========================================
-- 数据权限测试表
-- ========================================

-- 1. 部门表（用于测试部门权限）
CREATE TABLE IF NOT EXISTS `test_dept` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `dept_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父部门ID（0表示顶级部门）',
    `order_num` INT DEFAULT 0 COMMENT '显示顺序',
    `status` CHAR(1) DEFAULT '1' COMMENT '状态（1正常 0停用）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试部门表';

-- 2. 用户表（用于测试用户权限）
CREATE TABLE IF NOT EXISTS `test_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `user_name` VARCHAR(50) NOT NULL COMMENT '用户名',
    `nick_name` VARCHAR(50) COMMENT '昵称',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `status` CHAR(1) DEFAULT '1' COMMENT '状态（1正常 0停用）',
    `create_by` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用户表';

-- 3. 订单表（用于测试数据权限）
CREATE TABLE IF NOT EXISTS `test_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（创建人）',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID',
    `product_name` VARCHAR(100) COMMENT '产品名称',
    `amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '订单金额',
    `status` CHAR(1) DEFAULT '1' COMMENT '状态（1待支付 2已支付 3已取消）',
    `tenant_id` BIGINT COMMENT '租户ID（多租户场景）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试订单表';

-- ========================================
-- 初始化测试数据
-- ========================================

-- 1. 初始化部门数据（树形结构）
INSERT INTO `test_dept` (`id`, `dept_name`, `parent_id`, `order_num`, `status`) VALUES
(1, '总公司', 0, 1, '1'),
(2, '研发部', 1, 1, '1'),
(3, '市场部', 1, 2, '1'),
(4, '财务部', 1, 3, '1'),
(5, '后端组', 2, 1, '1'),
(6, '前端组', 2, 2, '1'),
(7, '测试组', 2, 3, '1'),
(8, '市场一部', 3, 1, '1'),
(9, '市场二部', 3, 2, '1');

-- 2. 初始化用户数据
INSERT INTO `test_user` (`id`, `user_name`, `nick_name`, `dept_id`, `phone`, `email`, `status`, `create_by`) VALUES
(1, 'admin', '超级管理员', 1, '13800000001', 'admin@test.com', '1', 1),
(2, 'dept_manager', '研发部经理', 2, '13800000002', 'manager@test.com', '1', 1),
(3, 'dept_leader', '后端组长', 5, '13800000003', 'leader@test.com', '1', 2),
(4, 'user1', '普通员工1', 5, '13800000004', 'user1@test.com', '1', 3),
(5, 'user2', '普通员工2', 5, '13800000005', 'user2@test.com', '1', 3),
(6, 'user3', '普通员工3', 6, '13800000006', 'user3@test.com', '1', 2),
(7, 'user4', '普通员工4', 8, '13800000007', 'user4@test.com', '1', 1),
(8, 'user5', '普通员工5', 9, '13800000008', 'user5@test.com', '1', 1);

-- 3. 初始化订单数据
INSERT INTO `test_order` (`id`, `order_no`, `user_id`, `dept_id`, `product_name`, `amount`, `status`, `tenant_id`) VALUES
(1, 'ORD20240101001', 4, 5, '产品A', 100.00, '2', 1),
(2, 'ORD20240101002', 4, 5, '产品B', 200.00, '2', 1),
(3, 'ORD20240101003', 5, 5, '产品C', 300.00, '1', 1),
(4, 'ORD20240101004', 5, 5, '产品D', 400.00, '2', 1),
(5, 'ORD20240101005', 6, 6, '产品E', 500.00, '1', 1),
(6, 'ORD20240101006', 6, 6, '产品F', 600.00, '2', 1),
(7, 'ORD20240101007', 7, 8, '产品G', 700.00, '1', 1),
(8, 'ORD20240101008', 7, 8, '产品H', 800.00, '2', 1),
(9, 'ORD20240101009', 8, 9, '产品I', 900.00, '1', 2),
(10, 'ORD20240101010', 8, 9, '产品J', 1000.00, '2', 2);

-- ========================================
-- 查询验证
-- ========================================

-- 查看部门树形结构
SELECT 
    d1.id AS dept_id,
    d1.dept_name,
    d1.parent_id,
    d2.dept_name AS parent_name
FROM test_dept d1
LEFT JOIN test_dept d2 ON d1.parent_id = d2.id
ORDER BY d1.parent_id, d1.order_num;

-- 查看用户及其部门
SELECT 
    u.id AS user_id,
    u.user_name,
    u.nick_name,
    d.dept_name,
    u.create_by
FROM test_user u
LEFT JOIN test_dept d ON u.dept_id = d.id
ORDER BY u.id;

-- 查看订单及其归属
SELECT 
    o.id AS order_id,
    o.order_no,
    u.user_name,
    d.dept_name,
    o.product_name,
    o.amount,
    o.tenant_id
FROM test_order o
LEFT JOIN test_user u ON o.user_id = u.id
LEFT JOIN test_dept d ON o.dept_id = d.id
ORDER BY o.id;

