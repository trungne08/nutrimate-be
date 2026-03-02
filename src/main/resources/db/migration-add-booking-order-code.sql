-- Migration: Thêm cột order_code cho bảng Bookings để map với PayOS orderCode
-- LƯU Ý:
-- - Cột để NULL được, nên an toàn cho DB đang chạy.
-- - Chỉ cần chạy một lần.

-- MySQL:
ALTER TABLE `Bookings`
  ADD COLUMN `order_code` BIGINT NULL;

