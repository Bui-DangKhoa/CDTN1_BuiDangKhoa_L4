# Test Plan — Luồng L4: Phân công KTV và Lịch hẹn (track SE)

## 1. Phạm vi
Kiểm: QT-08 (tay nghề, cùng trung tâm), QT-07 (lý do đổi KTV >= 10 ký tự),
QT-L4-01 (chống trùng lịch ±30 phút), QT-06 (vòng đời + ticket_status_log),
QT-15 (che SĐT), phân quyền theo chủ sở hữu, chạy lại seed không nhân đôi dữ liệu.
KHÔNG kiểm: gửi SMS/Zalo (ngoài phạm vi BT1); hiệu năng và tải (thuộc CĐTN2);
màn hình chỉ đọc không có quy tắc nghiệp vụ.

## 2. Loại test và số lượng
71 test tự động (JUnit 5 + Mockito/H2, chạy bằng ./mvnw test; BT2 ghi 70)
+ 12 test case chạy tay trên hệ thống Docker thật (giao diện, Swagger/Postman, MySQL).
Kỹ thuật chọn dữ liệu: phân vùng tương đương; giá trị biên cho QT-07 (9/10 ký tự)
và QT-L4-01 (29/30 phút).
Đủ 5 loại: luồng đúng · biên ×2 · dữ liệu sai · bảo mật · tái lập.

## 3. Tiêu chí thành công
- 12/12 test case có kết quả thực tế và ảnh chụp.
- Mỗi quy tắc nghiệp vụ đã hiện thực có ít nhất 1 test.
- Không còn bug Critical đang mở; bug Major còn lại có lý do và hướng xử lý.
- ./mvnw test: Failures 0, Errors 0.

## 4. Môi trường và dữ liệu thử
docker compose up -d --build · dữ liệu: seeds/data-demo.sql, seeds/auth_data.sql ·
tài khoản thử: quanly1, ktv1 · lệnh test tự động: ./mvnw test.