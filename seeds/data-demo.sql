-- Seed Dữ liệu Demo Nghiệp vụ (Smart CRM - Phân công KTV & Lịch hẹn)
-- Đảm bảo tính Idempotent (Tiêu chí C5): Sử dụng ON DUPLICATE KEY UPDATE / INSERT IGNORE

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;

-- -----------------------------------------------------
-- 1. Seed Danh mục sự cố kỹ thuật (issue_category)
-- -----------------------------------------------------
INSERT INTO `issue_category` (`category_id`, `category_name`, `default_priority`, `is_active`)
VALUES
(1, 'Thay màn hình cảm ứng', 'CAO', 1),
(2, 'Thay pin / Lỗi IC nguồn', 'CAO', 1),
(3, 'Lỗi phần mềm / Treo hệ điều hành', 'TRUNG_BINH', 1),
(4, 'Bảo dưỡng / Vệ sinh máy & Tra keo', 'THAP', 1),
(5, 'Sửa chữa Camera & Cụm cảm biến', 'TRUNG_BINH', 1)
ON DUPLICATE KEY UPDATE
    `category_name` = VALUES(`category_name`),
    `default_priority` = VALUES(`default_priority`),
    `is_active` = VALUES(`is_active`);

-- -----------------------------------------------------
-- 2. Seed Kỹ thuật viên (technician)
-- -----------------------------------------------------
INSERT INTO `technician` (`technician_id`, `employee_id`, `full_name`, `center_id`, `level`, `is_active`)
VALUES
(1, 1001, 'Trần Kỹ Thuật', 1, 'BAC_3', 1),
(2, 1002, 'Lê Văn An', 1, 'BAC_2', 1),
(3, 1003, 'Phạm Hoàng Bách', 1, 'BAC_1', 1),
(4, 1004, 'Đỗ Minh Tuấn', 2, 'BAC_2', 1)
ON DUPLICATE KEY UPDATE
    `employee_id` = VALUES(`employee_id`),
    `full_name` = VALUES(`full_name`),
    `center_id` = VALUES(`center_id`),
    `level` = VALUES(`level`),
    `is_active` = VALUES(`is_active`);

-- -----------------------------------------------------
-- 3. Seed Kỹ năng KTV (technician_skill - QT-08)
-- -----------------------------------------------------
INSERT INTO `technician_skill` (`technician_id`, `category_id`, `proficiency`)
VALUES
-- KTV 1 (Trần Kỹ Thuật - Bậc 3): Thành thạo cao tất cả danh mục
(1, 1, 5), (1, 2, 5), (1, 3, 4), (1, 4, 5), (1, 5, 4),
-- KTV 2 (Lê Văn An - Bậc 2): Chuyên phần cứng màn hình & pin
(2, 1, 4), (2, 2, 4), (2, 3, 3), (2, 4, 4), (2, 5, 2),
-- KTV 3 (Phạm Hoàng Bách - Bậc 1): KTV mới, chuyên phần mềm & vệ sinh
(3, 1, 2), (3, 2, 2), (3, 3, 4), (3, 4, 5), (3, 5, 1),
-- KTV 4 (Đỗ Minh Tuấn - Trung tâm 2)
(4, 1, 4), (4, 2, 3), (4, 3, 3), (4, 4, 4), (4, 5, 3)
ON DUPLICATE KEY UPDATE
    `proficiency` = VALUES(`proficiency`);

-- -----------------------------------------------------
-- 4. Seed Thiết bị khách hàng (device)
-- -----------------------------------------------------
INSERT INTO `device` (`device_id`, `customer_id`, `device_name`, `serial_no`, `purchase_date`, `warranty_months`)
VALUES
(1, 201, 'iPhone 15 Pro Max 256GB', 'IP15PM-VN-001', '2024-01-15', 12),
(2, 202, 'Samsung Galaxy S24 Ultra', 'SS24U-VN-002', '2024-03-20', 12),
(3, 203, 'MacBook Pro M3 14 inch', 'MBPM3-VN-003', '2023-11-10', 24),
(4, 204, 'iPad Air M2 11 inch', 'IPADAIR-VN-004', '2024-05-01', 12),
(5, 205, 'Xiaomi 14 Pro 512GB', 'MI14P-VN-005', '2024-02-18', 12)
ON DUPLICATE KEY UPDATE
    `customer_id` = VALUES(`customer_id`),
    `device_name` = VALUES(`device_name`),
    `serial_no` = VALUES(`serial_no`),
    `purchase_date` = VALUES(`purchase_date`),
    `warranty_months` = VALUES(`warranty_months`);

-- -----------------------------------------------------
-- 5. Seed Phiếu sửa chữa (ticket)
-- -----------------------------------------------------
INSERT INTO `ticket` (`ticket_id`, `ticket_code`, `customer_id`, `customer_phone`, `device_id`, `center_id`, `technician_id`, `category_id`, `issue_desc`, `resolution`, `priority`, `status`, `received_at`, `due_date`, `closed_at`, `deleted_at`)
VALUES
-- Phiếu 1: Chưa phân công, ưu tiên CAO (Sắp hết hạn SLA, dùng cho Quản lý test phân công)
(101, 'TK-202610-001', 201, '0987654321', 1, 1, NULL, 1, 'Màn hình bị sọc xanh và loạn cảm ứng sau va đập mạnh', NULL, 'CAO', 'MOI', '2026-10-20 08:30:00', '2026-10-22 17:00:00', NULL, NULL),
-- Phiếu 2: Chưa phân công, ưu tiên CAO
(102, 'TK-202610-002', 202, '0912345678', 2, 1, NULL, 2, 'Pin phù kênh nắp lưng, máy tự sập nguồn đột ngột khi dưới 30%', NULL, 'CAO', 'MOI', '2026-10-20 10:00:00', '2026-10-23 12:00:00', NULL, NULL),
-- Phiếu 3: Đã gán cho KTV 1, đang xử lý (ktv1 xem tại /technician/my-tickets)
(103, 'TK-202610-003', 203, '0909123456', 3, 1, 1, 3, 'Máy bị treo táo sau khi nâng cấp hệ điều hành macOS', NULL, 'TRUNG_BINH', 'DANG_XU_LY', '2026-10-19 14:00:00', '2026-10-24 18:00:00', NULL, NULL),
-- Phiếu 4: Đã gán cho KTV 1, đang xử lý
(104, 'TK-202610-004', 204, '0933221100', 4, 1, 1, 4, 'Bảo dưỡng định kỳ, kiểm tra chân sạc USB-C lỏng lẻo', NULL, 'THAP', 'DANG_XU_LY', '2026-10-19 15:00:00', '2026-10-25 10:00:00', NULL, NULL),
-- Phiếu 5: Đã gán cho KTV 2
(105, 'TK-202610-005', 205, '0977889900', 5, 1, 2, 5, 'Camera sau bị rung và mờ, không lấy nét quang học được', NULL, 'TRUNG_BINH', 'DA_PHAN_CONG', '2026-10-20 11:00:00', '2026-10-23 17:00:00', NULL, NULL),
-- Phiếu 6: Đã gán cho KTV 1 - Thay màn hình cảm ứng, trạng thái ĐÃ PHÂN CÔNG (dùng để test KTV bấm nhận việc / chuyển ĐANG_XU_LY)
(106, 'TK-202610-006', 201, '0987654321', 1, 1, 1, 1, 'Màn hình OLED bị ố vàng góc trên, cảm ứng đơ giật ngắt quãng', NULL, 'CAO', 'DA_PHAN_CONG', '2026-10-21 09:00:00', '2026-10-23 16:00:00', NULL, NULL),
-- Phiếu 7: Đã gán cho KTV 1 - Thay pin / Nguồn, trạng thái ĐANG XỬ LÝ (dùng để test KTV bấm hoàn tất sửa chữa HOAN_TAT)
(107, 'TK-202610-007', 202, '0912345678', 2, 1, 1, 2, 'Pin chai còn 68%, máy thường xuyên báo pin ảo và tự khởi động lại', NULL, 'CAO', 'DANG_XU_LY', '2026-10-21 10:30:00', '2026-10-24 15:00:00', NULL, NULL),
-- Phiếu 8: Đã gán cho KTV 1 - Sửa chữa Camera, trạng thái ĐÃ PHÂN CÔNG
(108, 'TK-202610-008', 205, '0977889900', 5, 1, 1, 5, 'Cụm camera tele zoom 5x bị mờ sương và kẹt chống rung OIS', NULL, 'TRUNG_BINH', 'DA_PHAN_CONG', '2026-10-21 14:00:00', '2026-10-25 12:00:00', NULL, NULL),
-- Phiếu cũ 1: Lịch sử sửa chữa thiết bị 1 (iPhone 15 Pro Max) - Đã đóng hoàn tất
(71, 'TK-202606-071', 201, '0987654321', 1, 1, 2, 1, 'Màn hình OLED bị ố sọc và liệt cảm ứng một nửa', 'Thay màn hình OLED chính hãng và dán keo chống nước viền', 'CAO', 'DONG', '2026-06-10 09:00:00', '2026-06-12 17:00:00', '2026-06-12 16:30:00', NULL),
-- Phiếu cũ 2: Lịch sử sửa chữa thiết bị 1 (iPhone 15 Pro Max) - Đã hoàn tất
(98, 'TK-202609-098', 201, '0987654321', 1, 1, 3, 1, 'Màn hình chớp nháy khi mở độ sáng cao sau va đập nhẹ', 'Thay lại cáp nối màn hình, vệ sinh socket và kiểm tra cảm ứng', 'TRUNG_BINH', 'HOAN_TAT', '2026-09-01 10:00:00', '2026-09-03 16:00:00', '2026-09-03 14:15:00', NULL),
-- Phiếu 9 (Mẫu tra cứu Cổng khách hàng /lookup): BH-00125 (SĐT: 0901234567)
(125, 'BH-00125', 201, '0901234567', 1, 1, 1, 1, 'Màn hình cảm ứng bị loạn, hẹn tiếp nhận kiểm tra và thay màn hình', NULL, 'CAO', 'DANG_XU_LY', '2026-10-25 08:30:00', '2026-10-28 17:00:00', NULL, NULL)
ON DUPLICATE KEY UPDATE
    `ticket_code` = VALUES(`ticket_code`),
    `customer_id` = VALUES(`customer_id`),
    `customer_phone` = VALUES(`customer_phone`),
    `device_id` = VALUES(`device_id`),
    `center_id` = VALUES(`center_id`),
    `technician_id` = VALUES(`technician_id`),
    `category_id` = VALUES(`category_id`),
    `issue_desc` = VALUES(`issue_desc`),
    `resolution` = VALUES(`resolution`),
    `priority` = VALUES(`priority`),
    `status` = VALUES(`status`),
    `received_at` = VALUES(`received_at`),
    `due_date` = VALUES(`due_date`),
    `closed_at` = VALUES(`closed_at`),
    `deleted_at` = VALUES(`deleted_at`);

-- -----------------------------------------------------
-- 6. Seed Lịch hẹn KTV (appointment - QT-L4-01 / NFR2 / US7 Cổng Tra Cứu Khách Hàng)
-- -----------------------------------------------------
INSERT INTO `appointment` (`appointment_id`, `ticket_id`, `technician_id`, `appointment_time`, `status`, `created_at`)
VALUES
-- Lịch hẹn cho Phiếu 103 (TK-202610-003, SĐT: 0909123456): Chờ khách hàng xác nhận qua cổng tra cứu /lookup
(1, 103, 1, '2026-10-27 09:30:00', 'CHO_XAC_NHAN', '2026-10-25 14:15:00'),
-- Lịch hẹn cho Phiếu 104 (TK-202610-004, SĐT: 0933221100): Đã xác nhận giao nhận
(2, 104, 1, '2026-10-27 14:00:00', 'DA_XAC_NHAN', '2026-10-25 15:30:00'),
-- Lịch hẹn cho Phiếu 106 (TK-202610-006, SĐT: 0987654321): KTV 1 giao nhận màn hình
(3, 106, 1, '2026-10-28 10:00:00', 'DA_XAC_NHAN', '2026-10-25 09:15:00'),
-- Lịch hẹn cho Phiếu 107 (TK-202610-007, SĐT: 0912345678): KTV 1 thay pin, chờ xác nhận
(4, 107, 1, '2026-10-29 14:30:00', 'CHO_XAC_NHAN', '2026-10-25 10:45:00'),
-- Lịch hẹn cho Phiếu 125 (BH-00125, SĐT: 0901234567): Mẫu chuẩn Cổng Tra Cứu Khách Hàng /lookup, chờ xác nhận
(5, 125, 1, '2026-10-28 15:00:00', 'CHO_XAC_NHAN', '2026-10-25 09:00:00'),
-- Lịch hẹn cho Phiếu 101 (TK-202610-001, SĐT: 0987654321): Đã xác nhận
(6, 101, 2, '2026-10-27 10:30:00', 'DA_XAC_NHAN', '2026-10-25 08:45:00')
ON DUPLICATE KEY UPDATE
    `ticket_id` = VALUES(`ticket_id`),
    `technician_id` = VALUES(`technician_id`),
    `appointment_time` = VALUES(`appointment_time`),
    `status` = VALUES(`status`),
    `created_at` = VALUES(`created_at`);

-- -----------------------------------------------------
-- 7. Seed Lịch sử trạng thái phiếu (ticket_status_log)
-- -----------------------------------------------------
INSERT INTO `ticket_status_log` (`log_id`, `ticket_id`, `from_status`, `to_status`, `changed_by`, `change_reason`, `changed_at`)
VALUES
(1, 103, 'MOI', 'CHO_TIEP_NHAN', 1, 'Quản lý tiếp nhận phiếu và phân công KTV Trần Kỹ Thuật', '2026-10-19 14:05:00'),
(2, 103, 'CHO_TIEP_NHAN', 'DANG_XU_LY', 1, 'KTV tiếp nhận và bắt đầu kiểm tra chuẩn đoán phần mềm', '2026-10-19 14:10:00')
ON DUPLICATE KEY UPDATE
    `ticket_id` = VALUES(`ticket_id`),
    `from_status` = VALUES(`from_status`),
    `to_status` = VALUES(`to_status`),
    `changed_by` = VALUES(`changed_by`),
    `change_reason` = VALUES(`change_reason`),
    `changed_at` = VALUES(`changed_at`);

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
