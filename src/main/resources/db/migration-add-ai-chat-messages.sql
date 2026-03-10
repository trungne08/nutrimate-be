-- Migration: Bảng AI_Chat_Messages lưu lịch sử chat User <-> AI Coach

CREATE TABLE IF NOT EXISTS `AI_Chat_Messages` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `role` VARCHAR(10) NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_ai_chat_user` FOREIGN KEY (`user_id`) REFERENCES `Users`(`user_id`) ON DELETE CASCADE
);
