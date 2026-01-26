# 🔄 Hướng Dẫn Migration ID từ Long sang String (UUID)

## ⚠️ Lưu Ý Quan Trọng

Sau khi đổi ID từ `Long` sang `String` (UUID), bạn **PHẢI** cập nhật database schema thủ công vì Hibernate không thể tự động migrate kiểu dữ liệu.

## 📋 Các Thay Đổi Đã Thực Hiện

### 1. Entity Changes
- ✅ **`User.id`**: `Long` → `String` (UUID, VARCHAR(36))
- ✅ **`HealthProfile.id`**: `Long` → `String` (UUID, VARCHAR(36))
- ✅ **`HealthProfile.userId`**: `Long` → `String` (UUID, VARCHAR(36))

**Lưu ý**: Cả 2 bảng `Users` và `Health_Profiles` đều đã được cập nhật để dùng UUID String thay vì Long Integer.

### 2. Repository Changes
- ✅ `UserRepository extends JpaRepository<User, String>`
- ✅ `HealthProfileRepository extends JpaRepository<HealthProfile, String>`
- ✅ `findByUserId(String userId)` - đã cập nhật

### 3. Controllers & Services
- ✅ Không cần thay đổi (dùng `getId()` tự động)

## 🗄️ SQL Migration Script

**⚠️ QUAN TRỌNG**: Cả 2 bảng `Users` và `Health_Profiles` đều cần đổi ID từ `INT` sang `VARCHAR(36)` (UUID).

Chạy các lệnh SQL sau trong database để migrate:

```sql
-- ============================================
-- BƯỚC 1: Xóa foreign key constraint cũ
-- ============================================
ALTER TABLE Health_Profiles DROP FOREIGN KEY IF EXISTS FKlya5bm3wyg1qa0h1hace0chva;
ALTER TABLE Health_Profiles DROP FOREIGN KEY IF EXISTS health_profiles_ibfk_1;

-- ============================================
-- BƯỚC 2: Xóa các bảng cũ (⚠️ BACKUP DATA TRƯỚC!)
-- ============================================
-- Nếu bạn muốn giữ data, cần migrate data thủ công (xem phần dưới)
DROP TABLE IF EXISTS Health_Profiles;
DROP TABLE IF EXISTS Users;

-- ============================================
-- BƯỚC 3: Tạo lại bảng Users với user_id là VARCHAR(36) (UUID)
-- ============================================
CREATE TABLE `Users` (
    `user_id` VARCHAR(36) PRIMARY KEY,  -- 👈 Đã đổi từ INT sang VARCHAR(36)
    `cognito_id` VARCHAR(255) UNIQUE,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `full_name` VARCHAR(100),
    `username` VARCHAR(100),
    `phone_number` VARCHAR(15),
    `role` ENUM('MEMBER', 'EXPERT', 'ADMIN') NOT NULL DEFAULT 'MEMBER',
    `avatar_url` VARCHAR(255),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================
-- BƯỚC 4: Tạo lại bảng Health_Profiles với VARCHAR(36) (UUID)
-- ============================================
CREATE TABLE `Health_Profiles` (
    `profile_id` VARCHAR(36) PRIMARY KEY,  -- 👈 Đã đổi từ INT sang VARCHAR(36)
    `user_id` VARCHAR(36) NOT NULL,       -- 👈 Đã đổi từ INT sang VARCHAR(36)
    `gender` ENUM('Male', 'Female', 'Other'),
    `date_of_birth` DATE,
    `height_cm` FLOAT,
    `weight_kg` FLOAT,
    `target_weight_kg` FLOAT,
    `activity_level` VARCHAR(50),
    `dietary_preference` VARCHAR(50) DEFAULT 'Clean Eating',
    FOREIGN KEY (`user_id`) REFERENCES `Users`(`user_id`) ON DELETE CASCADE
);
```

**✅ Sau khi chạy script này:**
- Bảng `Users` sẽ có `user_id` là `VARCHAR(36)` (UUID)
- Bảng `Health_Profiles` sẽ có `profile_id` và `user_id` là `VARCHAR(36)` (UUID)
- Foreign key constraint sẽ hoạt động vì cả 2 cột đều cùng kiểu `VARCHAR(36)`

## 🔄 Nếu Muốn Giữ Data Cũ

Nếu bạn muốn giữ data hiện tại, cần migrate thủ công:

```sql
-- 1. Tạo bảng tạm để backup
CREATE TABLE Users_backup AS SELECT * FROM Users;
CREATE TABLE Health_Profiles_backup AS SELECT * FROM Health_Profiles;

-- 2. Tạo bảng mới với UUID
-- (Dùng script ở trên)

-- 3. Migrate data (generate UUID cho mỗi record)
INSERT INTO `Users` (`user_id`, `cognito_id`, `email`, `full_name`, `username`, `phone_number`, `role`, `avatar_url`, `created_at`, `updated_at`)
SELECT 
    UUID() as user_id,  -- Generate UUID mới
    cognito_id,
    email,
    full_name,
    username,
    phone_number,
    role,
    avatar_url,
    created_at,
    updated_at
FROM Users_backup;

-- 4. Migrate Health_Profiles (cần map user_id cũ sang UUID mới)
-- ⚠️ Phức tạp hơn, cần tạo mapping table
```

## ✅ Sau Khi Migration

1. **Restart Application**: Restart Spring Boot app
2. **Test**: 
   - Đăng nhập lại (sẽ tạo user mới với UUID)
   - Tạo health profile mới
   - Kiểm tra foreign key constraint hoạt động

## 🐛 Troubleshooting

### Lỗi: Foreign key constraint incompatible
- **Nguyên nhân**: Database vẫn dùng INT cho user_id
- **Giải pháp**: Chạy SQL migration script ở trên

### Lỗi: Column type mismatch
- **Nguyên nhân**: Hibernate đang cố tạo bảng với VARCHAR nhưng DB có INT
- **Giải pháp**: Set `spring.jpa.hibernate.ddl-auto=create` tạm thời (⚠️ sẽ xóa data!) hoặc migrate thủ công

### Lỗi: UUID generation failed
- **Nguyên nhân**: Thiếu dependency hoặc config
- **Giải pháp**: Hibernate 5+ đã có sẵn UUID generator, không cần thêm dependency

## 📝 Lưu Ý

- UUID có độ dài 36 ký tự (format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)
- Foreign key constraint giờ sẽ hoạt động vì cả 2 cột đều là VARCHAR(36)
- Performance: UUID hơi chậm hơn INT nhưng tốt cho distributed systems

---

**Sau khi migration xong, app sẽ tự động generate UUID cho mọi user và health profile mới!** 🚀
