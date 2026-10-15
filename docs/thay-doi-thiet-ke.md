# Nhật ký thay đổi thiết kế (Design Change Log)

## Sau phản hồi Bài tập 1 (Buổi 8 / Sprint 1)

### TD-01: Bổ sung bảng device và cột full_name trong bảng technician
- **Phản hồi của GV**: Wireframe hiển thị tên thiết bị và họ tên kỹ thuật viên nhưng ERD không có bảng device và bảng technician không có cột full_name, trong khi Bảng đối chiếu 5.2 lại ghi technician.full_name.
- **Quyết định**: SỬA.
- **Lý do**: Đồng bộ dữ liệu 100% giữa Giao diện (Wireframe) ↔ CSDL (ERD/DDL) ↔ Bảng truy vết 5.2 của SRS.
- **Ảnh hưởng**: 
  - Bổ sung bảng `device` và cột `full_name` vào file `docs/schema.sql`.
  - Cập nhật sơ đồ `docs/erd.drawio`.
  - Đổi khóa ngoại `ticket.device_id` trỏ trực tiếp tới `device(device_id)`.

### TD-02: Bổ sung Chú thích ký hiệu (Legend) cho ERD
- **Phản hồi của GV**: ERD chưa có chú thích ký hiệu.
- **Quyết định**: SỬA.
- **Lý do**: Đảm bảo sơ đồ mô hình dữ liệu đọc được độc lập mà không cần tác giả giải thích thêm (đạt tiêu chí kỹ thuật BT1/BT2).
- **Ảnh hưởng**: Bổ sung khung Legend ký pháp chân chim (Crow's Foot) ở góc sơ đồ `docs/erd.drawio`.

### TD-03: Chuẩn hóa Sơ đồ Use Case và Bảng đối chiếu 5.2
- **Phản hồi của GV**: Sơ đồ Use Case không ghi mã UC3, UC7. Quan hệ extend giữa UC2–UC1, UC5–UC4 dùng chưa đúng. Bảng đối chiếu ghi FR5 ↔ US6.
- **Quyết định**: SỬA.
- **Lý do**: Chuẩn hóa ký pháp UML và sửa lỗi đánh máy liên kết truy vết.
- **Ảnh hưởng**:
  - Điền rõ nhãn `UC3` và `UC7` trong `docs/usecase.drawio`.
  - Bỏ mũi tên `<<extend>>` dùng sai, thay bằng liên kết trực tiếp giữa Actor và Use Case.
  - Sửa dòng đối chiếu trong `docs/srs.md` từ `FR5 ↔ US6` thành `FR5 ↔ US5`.

### TD-04: Bổ sung các tệp còn thiếu vào GitHub Repository
- **Phản hồi của GV**: Repo thiếu wireframe và schema.sql dù bài có nhắc tới.
- **Quyết định**: SỬA.
- **Lý do**: Đảm bảo repo đầy đủ tài liệu phục vụ việc clone & kiểm chứng của giảng viên.
- **Ảnh hưởng**: Thêm tệp `docs/schema.sql` (MySQL DDL) và `docs/wireframe.png` vào thư mục `docs/`.