-- Migration: Thêm trạng thái DONE vào cột status của bảng Bookings
-- LƯU Ý: Chạy script này sau khi deploy code enum BookingStatus mới.

-- MySQL:
ALTER TABLE `Bookings`
  MODIFY COLUMN `status` ENUM('PENDING','CONFIRMED','REJECTED','COMPLETED','DONE','CANCELLED') NOT NULL;

