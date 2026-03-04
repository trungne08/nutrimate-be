-- Migration: Fix created_at NULL - Cập nhật tất cả record cũ có created_at IS NULL thành NOW()
-- Giúp biểu đồ Frontend không bị lỗi do dữ liệu null.
-- Chạy script này một lần sau khi backup DB.

-- ========== 1. Thêm cột created_at/updated_at (chỉ chạy nếu bảng chưa có cột) ==========
-- Nếu báo lỗi "Duplicate column name" thì bỏ qua - cột đã tồn tại.

ALTER TABLE `User_Subscriptions` ADD COLUMN `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `User_Subscriptions` ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `Payments` ADD COLUMN `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `Payments` ADD COLUMN `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- ========== 2. UPDATE tất cả record có created_at NULL -> NOW() (CỨU BIỂU ĐỒ) ==========

UPDATE `Users` SET `created_at` = NOW() WHERE `created_at` IS NULL;
UPDATE `Users` SET `updated_at` = NOW() WHERE `updated_at` IS NULL;

UPDATE `Bookings` SET `created_at` = NOW() WHERE `created_at` IS NULL;

UPDATE `User_Subscriptions` SET `created_at` = NOW() WHERE `created_at` IS NULL;
UPDATE `User_Subscriptions` SET `updated_at` = NOW() WHERE `updated_at` IS NULL;

UPDATE `Payments` SET `created_at` = NOW() WHERE `created_at` IS NULL;
UPDATE `Payments` SET `updated_at` = NOW() WHERE `updated_at` IS NULL;

UPDATE `Posts` SET `created_at` = NOW() WHERE `created_at` IS NULL;
UPDATE `Posts` SET `updated_at` = NOW() WHERE `updated_at` IS NULL;

UPDATE `Comments` SET `created_at` = NOW() WHERE `created_at` IS NULL;

UPDATE `feedbacks` SET `created_at` = NOW() WHERE `created_at` IS NULL;

UPDATE `system_feedbacks` SET `created_at` = NOW() WHERE `created_at` IS NULL;

UPDATE `Payment_Order_Mappings` SET `created_at` = NOW() WHERE `created_at` IS NULL;
