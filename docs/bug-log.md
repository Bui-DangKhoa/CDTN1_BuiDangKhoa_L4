# Nhật ký gỡ lỗi (Bug Log)
> **Dự án**: CDTN1_BuiDangKhoa_L4 (Hệ thống điều phối kỹ thuật viên Smart CRM - Mekong Mobile)  
> **Quy trình gỡ lỗi chuẩn 4 bước**: Tái lập (Reproduce) ➔ Khoanh vùng (Isolate) ➔ Sửa nguyên nhân (Fix Root Cause) ➔ Xác nhận bằng test (Verify via Regression Test).

---

## BUG-001 · CRITICAL — Lỗ hổng phân quyền OWASP A01 (Sửa phiếu của KTV khác)
1. Mã và mức độ  : BUG-001 · CRITICAL — lỗ hổng kiểm soát truy cập, dữ liệu bị thay đổi trái phép.
2. Phát hiện bởi : TC-09 (Test Report - Kiểm thử bảo mật).
3. Triệu chứng   : KTV A đăng nhập vẫn sửa được trạng thái phiếu bảo hành gán cho KTV B. API trả về 200 OK.
4. Cách tái lập  : Lấy Bearer Token của KTV A, gọi `PATCH /api/v1/tickets/105/status` (phiếu 105 thuộc KTV B).
5. Nguyên nhân   : `TicketServiceImpl.updateStatus()` chỉ kiểm tra phiếu tồn tại, thiếu kiểm tra `ticket.getTechnicianId().equals(currentUserId)`.
6. Cách sửa      : Thêm kiểm tra chủ sở hữu trong `TicketServiceImpl.java`; ném `AppException("KHONG_CO_QUYEN")` (máy dịch thành HTTP 403 ở Controller).
7. Commit & xác nhận : Commit [af86e4d](https://github.com/Bui-DangKhoa/CDTN1_BuiDangKhoa_L4/commit/af86e4d) · Chạy lại TC-09 -> PASS (HTTP 403 Forbidden).

### Chi tiết các bước gỡ lỗi theo chuẩn 4 bước (BUG-001):

1. **Bước 1: Tái lập (Reproduce)**
   - Thực thi kiểm thử bảo mật phân quyền hoặc gọi API đổi trạng thái:
     ```bash
     curl -X PATCH http://localhost:8080/api/v1/tickets/105/status \
       -H "Authorization: Bearer <TOKEN_KTV_A>" \
       -H "Content-Type: application/json" \
       -d '{"newStatus": "DANG_XU_LY"}'
     ```
   - Phiếu `105` vốn được gán cho KTV B (ID = 2), nhưng KTV A (ID = 1) vẫn cập nhật thành công, API trả về `200 OK`.
   - Đồng thời, tài khoản chưa liên kết hồ sơ KTV (`userId = 99`) khi truy cập bị fallback về `findById(1L)`, thấy toàn bộ phiếu của KTV 1:
     `./mvnw test -Dtest=TechnicianAccessRegressionTest`

2. **Bước 2: Khoanh vùng (Isolate)**
   - **Vị trí 1**: Phương thức `updateTicketStatus(...)` trong `TicketServiceImpl.java` chỉ truy vấn phiếu theo ID và kiểm tra hợp lệ vòng đời trạng thái QT-06, hoàn toàn bỏ qua việc đối chiếu `ticket.getTechnicianId()` với `currentUserId`.
   - **Vị trí 2**: Phương thức `resolveTechnicianForUser(...)` trong `TechnicianServiceImpl.java` có đoạn mã fallback nguy hiểm:
     ```java
     if (tech == null) {
         tech = technicianRepository.findById(1L).orElse(null); // Fallback mặc định về KTV 1
     }
     ```

3. **Bước 3: Sửa nguyên nhân gốc (Fix Root Cause)**
   - **Loại bỏ fallback gán cứng**: Sửa `resolveTechnicianForUser` thành tìm chính xác theo `findByUserId(currentUser.getUserId())` và trả về `Optional<Technician>`. Nếu không tìm thấy, trả về `Optional.empty()` thay vì `1L`.
   - **Kiểm soát quyền sở hữu phiếu**: Trong `TicketServiceImpl.java`, kiểm tra KTV thực hiện thao tác phải trùng khớp với KTV được gán trên phiếu:
     ```java
     if (currentUserRole.equals("KY_THUAT_VIEN") && !ticket.getTechnicianId().equals(currentUserId)) {
         log.warn("KTV ID {} cố tình thao tác trên phiếu #{} của KTV ID {}", currentUserId, ticket.getTicketCode(), ticket.getTechnicianId());
         throw new AppException("KHONG_CO_QUYEN", "Bạn không có quyền cập nhật phiếu bảo hành của kỹ thuật viên khác");
     }
     ```
   - `GlobalExceptionHandler` bắt mã lỗi `KHONG_CO_QUYEN` / `FORBIDDEN` và chuyển đổi thành HTTP Status `403 FORBIDDEN`.

4. **Bước 4: Xác nhận bằng test (Verify via Regression Test)**
   - Chạy test kiểm thử bảo mật `TC-09` trong `AuthAndSecurityTest` và kiểm thử hồi quy `TechnicianAccessRegressionTest`:
     ```powershell
     .\mvnw.cmd test -Dtest=TechnicianAccessRegressionTest
     .\mvnw.cmd test -Dtest=AuthAndSecurityTest
     ```
   - Kết quả: `BUILD SUCCESS` (PASS 100%).
   - KTV A không thể cập nhật phiếu của KTV B (nhận về HTTP `403 Forbidden`). Tài khoản chưa liên kết nhận thông báo an toàn, không bị lộ dữ liệu phiếu của KTV khác.

---

## BUG-002 · MAJOR — Sập 500 khi khởi tạo Lịch hẹn trùng khung giờ (QT-L4-01)
1. Mã và mức độ  : BUG-002 · MAJOR — chức năng đặt lịch hẹn ném 500 Unhandled Exception thay vì thông báo lỗi nghiệp vụ.
2. Phát hiện bởi : TC-06 (Invalid Data test case).
3. Triệu chứng   : Gọi `POST /api/v1/appointments` đặt lịch hẹn trùng giờ, hệ thống văng vệt lỗi `DataIntegrityViolationException`.
4. Cách tái lập  : Gửi request tạo lịch hẹn cho KTV 1 vào lúc 14:15 ngày 30/10/2026 (đã có lịch hẹn 14:00 trước đó).
5. Nguyên nhân   : `AppointmentServiceImpl` thiếu hàm kiểm tra khoảng thời gian trùng lắp `hasScheduleConflict()` ở Tầng Service trước khi lưu CSDL.
6. Cách sửa      : Thêm truy vấn JPQL `existsOverlappingAppointment()` trong Repository và kiểm tra trong Service; ném `AppException("SCHEDULE_CONFLICT")`.
7. Commit & xác nhận : Commit [4b866a2](https://github.com/Bui-DangKhoa/CDTN1_BuiDangKhoa_L4/commit/4b866a2) · Chạy lại TC-06 -> PASS (HTTP 409 Conflict).

### Chi tiết các bước gỡ lỗi theo chuẩn 4 bước (BUG-002):

1. **Bước 1: Tái lập (Reproduce)**
   - Thực thi lệnh kiểm thử tự động:
     ```powershell
     .\mvnw.cmd test -Dtest=AppointmentServiceTest#testCreateAppointment_ScheduleConflict_QTL401
     ```
   - Hoặc gửi yêu cầu HTTP đặt lịch hẹn trùng giờ qua curl:
     ```bash
     curl -X POST http://localhost:8080/api/v1/appointments \
       -H "Content-Type: application/json" \
       -d '{"ticketId": 102, "technicianId": 1, "appointmentTime": "2026-10-30T14:15:00"}'
     ```
   - Hệ thống văng ngoại lệ `DataIntegrityViolationException` (HTTP 500 Internal Server Error) do xung đột dữ liệu thay vì trả về thông báo nghiệp vụ thân thiện cho người dùng.

2. **Bước 2: Khoanh vùng (Isolate)**
   - Vị trí lỗi: Phương thức `scheduleAppointment(Long ticketId, Long technicianId, LocalDateTime appointmentTime)` trong `AppointmentService.java`.
   - Tầng dữ liệu: `AppointmentRepository` chưa có phương thức kiểm tra trùng khung giờ có loại trừ các lịch hẹn đã hủy (`DA_HUY`).

3. **Bước 3: Sửa nguyên nhân gốc (Fix Root Cause)**
   - Bổ sung phương thức truy vấn Spring Data JPA trong `AppointmentRepository.java`:
     ```java
     boolean existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(
         Long technicianId, LocalDateTime appointmentTime, String status
     );
     ```
   - Trước khi lưu lịch hẹn mới, kiểm tra và ném ngoại lệ nghiệp vụ tại tầng Service:
     ```java
     boolean isConflict = appointmentRepository.existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(
         technicianId, appointmentTime, "DA_HUY"
     );
     if (isConflict) {
         log.warn("Phát hiện xung đột lịch hẹn: KTV ID {} đã có lịch vào lúc {}", technicianId, appointmentTime);
         throw new AppException("SCHEDULE_CONFLICT", "Kỹ thuật viên đã có lịch hẹn khác trong khung giờ này");
     }
     ```
   - `GlobalExceptionHandler` ánh xạ mã lỗi `SCHEDULE_CONFLICT` sang mã trạng thái chuẩn HTTP `409 CONFLICT`.

4. **Bước 4: Xác nhận bằng test (Verify via Regression Test)**
   - Kiểm thử đơn vị `AppointmentServiceTest#testCreateAppointment_ScheduleConflict_QTL401` thực thi và trả về kết quả `BUILD SUCCESS` (Pass 100%):
     ```powershell
     .\mvnw.cmd test -Dtest=AppointmentServiceTest#testCreateAppointment_ScheduleConflict_QTL401
     ```
   - Ngoại lệ trả về đúng mã lỗi `SCHEDULE_CONFLICT` với HTTP Status `409 CONFLICT`, không lộ stack trace hay câu lệnh SQL nội bộ ra client.
