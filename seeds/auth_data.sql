-- Seed Data Tài khoản Người dùng (Authentication & Authorization)
-- Mật khẩu gốc: 'password123' đã được mã hóa bằng chuẩn BCrypt (Cost factor: 10)
-- Hash: $2a$10$e8R4a1000000000000000uUsHL.G5k4HzJ.r5f0svoc62GUp/LD2O

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
INSERT INTO `app_user` (`user_id`, `username`, `password`, `full_name`, `role`, `center_id`, `is_active`)
VALUES 
(1, 'quanly1', '$2a$10$e8R4a1000000000000000uUsHL.G5k4HzJ.r5f0svoc62GUp/LD2O', 'Nguyễn Quản Lý', 'QUAN_LY', 1, 1),
(2, 'ktv1', '$2a$10$e8R4a1000000000000000uUsHL.G5k4HzJ.r5f0svoc62GUp/LD2O', 'Trần Kỹ Thuật', 'KY_THUAT_VIEN', 1, 1)
ON DUPLICATE KEY UPDATE 
    `password` = VALUES(`password`),
    `full_name` = VALUES(`full_name`),
    `role` = VALUES(`role`),
    `center_id` = VALUES(`center_id`),
    `is_active` = VALUES(`is_active`);
