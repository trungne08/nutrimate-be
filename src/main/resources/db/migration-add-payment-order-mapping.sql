-- Migration: Bảng Payment_Order_Mappings để map orderCode (PayOS) -> userId, planId
-- Dùng cho thanh toán Subscription: khi tạo link thanh toán lưu mapping, khi webhook trả về tạo UserSubscription.

CREATE TABLE IF NOT EXISTS `Payment_Order_Mappings` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `order_code` BIGINT NOT NULL UNIQUE,
    `user_id` VARCHAR(36) NOT NULL,
    `plan_id` VARCHAR(36) NOT NULL,
    `amount` BIGINT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_order_code` (`order_code`)
);
