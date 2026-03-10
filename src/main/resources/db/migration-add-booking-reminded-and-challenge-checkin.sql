-- Migration: Thêm cờ is_reminded cho Bookings, last_check_in_date cho User_Challenges

ALTER TABLE `Bookings` ADD COLUMN `is_reminded` TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE `User_Challenges` ADD COLUMN `last_check_in_date` DATE NULL;
