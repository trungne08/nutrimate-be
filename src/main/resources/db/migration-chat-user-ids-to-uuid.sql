-- Migration: Đổi sender_id, receiver_id từ BIGINT sang VARCHAR(36) (UUID)
-- Chạy script này nếu bảng Chat_Messages đã tồn tại với kiểu BIGINT.
--
-- LƯU Ý:
-- - Nếu có dữ liệu cũ (Long), cần backup và xử lý trước khi chạy.
-- - Nếu bảng mới / chưa có dữ liệu chat, có thể chạy trực tiếp.

-- MySQL:
ALTER TABLE `Chat_Messages`
  MODIFY COLUMN `sender_id` VARCHAR(36) NOT NULL,
  MODIFY COLUMN `receiver_id` VARCHAR(36) NOT NULL;
