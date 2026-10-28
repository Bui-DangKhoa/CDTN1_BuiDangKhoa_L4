# Nhật ký Gỡ lỗi Hệ thống Smart CRM (Luồng L4)
> **Dự án**: CDTN1_BuiDangKhoa_L4 (Hệ thống điều phối kỹ thuật viên Smart CRM - Mekong Mobile)  
> **Quy trình gỡ lỗi chuẩn 4 bước**: Tái lập (Reproduce) ➔ Khoanh vùng (Isolate) ➔ Sửa nguyên nhân (Fix Root Cause) ➔ Xác nhận bằng test (Verify via Regression Test).

---

## BUG-001 · Major — Lỗi sập hệ thống khi gán KTV trùng khung giờ hoặc thiếu lý do đổi

| Dòng | Nội dung |
| :--- | :--- |
| **1. Mã và mức độ** | BUG-001 · Major |
| **2. Phát hiện bởi** | Kiểm thử tự động `AppointmentServiceTest.testCreateAppointment_ScheduleConflict_QTL401` |
| **3. Triệu chứng** | Khởi tạo lịch hẹn trùng giờ ném 500 thay vì thông báo bận lịch |
| **4. Cách tái lập** | `./mvnw test -Dtest=AppointmentServiceTest#testCreateAppointment_ScheduleConflict_QTL401` |
| **5. Nguyên nhân** | Thiếu hàm kiểm tra chống trùng lịch `hasScheduleConflict` ở Tầng Service trước khi lưu DB |
| **6. Cách sửa** | Bổ sung truy vấn JPQL kiểm tra khoảng thời gian và ném `AppException("SCHEDULE_CONFLICT")` |
| **7. Commit và xác nhận** | Commit: `4b866a2` (Chống gán trùng lịch hẹn KTV QT-L4-01) + Unit test `AppointmentServiceTest#testCreateAppointment_ScheduleConflict_QTL401` PASS 100% |

---

### Chi tiết các bước gỡ lỗi theo chuẩn 4 bước:

1. **Bước 1: Tái lập (Reproduce)**
   - Thực thi `./mvnw test -Dtest=AppointmentServiceTest#testCreateAppointment_ScheduleConflict_QTL401`.
   - Giả lập trường hợp KTV `ID = 5` đã có lịch hẹn ở thời điểm `2026-10-20T09:00:00`.
   - Hệ thống không chặn ở tầng nghiệp vụ mà đẩy trực tiếp thao tác ghi xuống DB gây lỗi hoặc ghi đè lịch hẹn.

2. **Bước 2: Khoanh vùng (Isolate)**
   - Vị trí lỗi: Phương thức `scheduleAppointment(Long ticketId, Long technicianId, LocalDateTime appointmentTime)` trong `AppointmentService.java`.
   - Tầng dữ liệu: `AppointmentRepository` chưa có phương thức kiểm tra trùng lịch có loại trừ trạng thái `DA_HUY`.

3. **Bước 3: Sửa nguyên nhân gốc (Fix Root Cause)**
   - Thêm phương thức truy vấn Spring Data JPA:
     ```java
     boolean existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(
         Long technicianId, LocalDateTime appointmentTime, String status
     );
     ```
   - Trước khi lưu lịch hẹn, kiểm tra:
     ```java
     boolean isConflict = appointmentRepository.existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(
         technicianId, appointmentTime, "DA_HUY"
     );
     if (isConflict) {
         throw new AppException("SCHEDULE_CONFLICT", "Kỹ thuật viên đã có lịch hẹn khác trong khung giờ này");
     }
     ```

4. **Bước 4: Xác nhận bằng test (Verify via Regression Test)**
   - Kiểm thử đơn vị `AppointmentServiceTest#testCreateAppointment_ScheduleConflict_QTL401` thực thi và trả về kết quả `BUILD SUCCESS` (Pass 100%).
   - Ngoại lệ trả về đúng mã lỗi `SCHEDULE_CONFLICT` với HTTP Status `409 CONFLICT`, không lộ stack trace hay câu lệnh SQL nội bộ.
