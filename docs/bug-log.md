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

---

## BUG-02 · MAJOR — Lệch múi giờ UTC vs UTC+7, hiển thị sai nhãn SLA và cho chọn ngày hẹn quá khứ (QT-L4-01)
1. Mã và mức độ: BUG-02 · MAJOR — Lỗi lệch múi giờ UTC/UTC+7, tính sai SLA và thiếu ràng buộc thời gian hẹn trong quá khứ.
2. Phát hiện bởi: TC-08 (Phân loại SLA) và Kiểm thử giao diện Màn hình 2 (Phần 2 — Đặt lịch hẹn).
3. Triệu chứng:
   - Phiếu bảo hành có `due_date` trong quá khứ hiển thị "Sắp quá hạn" thay vì "Quá hạn".
   - Form phân công Phần 2 cho phép chọn ngày hẹn giao – nhận trong quá khứ (`modalAppointmentDate` mặc định `2026-10-30` không chặn min).
   - Thời gian hệ thống ghi nhật ký / log hiển thị giờ UTC (ví dụ `12:38:02`) thay vì giờ địa phương UTC+7 `Asia/Ho_Chi_Minh` (`18:38:02` / `19:38:02`).
4. Cách tái lập:
   - Mở `/tickets/unassigned`, phiếu quá hạn hiển thị "Sắp quá hạn".
   - Bấm "Phân công", ô ngày hẹn hiển thị ngày cũ `2026-10-30` và cho phép chọn ngày trong quá khứ rồi bấm "Lưu phân công & Đặt lịch" thành công.
   - Kiểm tra `ticket_status_log` hoặc `appointment` thấy `changed_at` / `created_at` lệch 7 tiếng (UTC).
5. Nguyên nhân:
   - JVM / Container và MySQL chưa được ép múi giờ `Asia/Ho_Chi_Minh` (`+07:00`), dẫn đến `LocalDateTime.now()` và `CURRENT_TIMESTAMP` lấy giờ UTC.
   - Hàm `getSlaStatus` không kiểm tra `now > dueDate` trước khi tính khoảng cảnh báo.
   - Các trường input `appointmentDate` ghi cứng giá trị quá khứ `2026-10-30`, thiếu thuộc tính `min` và thiếu validation kiểm tra `appointmentTime.isBefore(now)`.
6. Cách sửa:
   - Đặt múi giờ `Asia/Ho_Chi_Minh` cho JVM (`SmartCrmApplication`, `Dockerfile`, `docker-compose.yml`), MySQL (`--default-time-zone=+07:00`), Hibernate JDBC (`hibernate.jdbc.time_zone: Asia/Ho_Chi_Minh`) và Jackson serializer ISO 8601 kèm offset `+07:00`.
   - Cập nhật `getSlaStatus()` kiểm tra `now.isAfter(dueDate)` trả về `QUA_HAN` đầu tiên.
   - Loại bỏ giá trị quá khứ, thêm `min` động cho ô chọn ngày; bổ sung kiểm tra Javascript ở frontend và validation `@FutureOrPresent` / `appointmentTime.isBefore(now)` ở backend (ném `AppException("INVALID_APPOINTMENT_TIME")`).
7. Commit & xác nhận: Chạy lại toàn bộ bộ test `.\mvnw.cmd test` (81/81 test PASS 100%).

### Chi tiết các bước gỡ lỗi theo chuẩn 4 bước (BUG-02):

1. **Bước 1: Tái lập (Reproduce)**
   - Kiểm tra phiếu `#TK-202610-001` có `due_date: 2026-10-22` khi `now` là tháng 11/2026 vẫn bị dán nhãn "Sắp quá hạn".
   - Mở modal phân công tại `/tickets/unassigned`: Trường ngày hẹn mặc định là `2026-10-30` (ngày trong quá khứ) và không có thuộc tính `min`, người dùng có thể chọn bất kỳ ngày nào trong quá khứ và gửi form thành công.
   - Timestamp trong CSDL và phản hồi API trả về `12:38:02` thay vì `18:38:02` / `19:38:02` (+07:00).

2. **Bước 2: Khoanh vùng (Isolate)**
   - Vị trí 1: `TicketServiceImpl.getSlaStatus()` tính toán khoảng cách giờ mà không xét `now > dueDate` trước.
   - Vị trí 2: Form giao diện `unassigned-list.html`, `assign-form.html`, `assign-modal.html` ghi cứng `value="2026-10-30"`, thiếu `min`.
   - Vị trí 3: `TicketController.assignTechnician()` và `TicketServiceImpl.assignTechnician()` chưa kiểm tra `appointmentTime` có ở trong quá khứ hay không.
   - Vị trí 4: Cấu hình múi giờ ở `SmartCrmApplication`, `application.yml`, `docker-compose.yml`, `Dockerfile` chưa đồng bộ `Asia/Ho_Chi_Minh` và `--default-time-zone=+07:00`.

3. **Bước 3: Sửa nguyên nhân gốc (Fix Root Cause)**
   - **SLA & Timezone**:
     - Cấu hình static block + `@PostConstruct` trong `SmartCrmApplication`: `TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))`.
     - Thêm `hibernate.jdbc.time_zone: Asia/Ho_Chi_Minh`, `serverTimezone=Asia/Ho_Chi_Minh`, `TZ=Asia/Ho_Chi_Minh` và `--default-time-zone=+07:00` trong Docker.
     - Cập nhật `TicketServiceImpl.getSlaStatus()`: Ưu tiên số 1 `if (now.isAfter(dueDate)) return "QUA_HAN";`.
   - **Chống chọn ngày trong quá khứ (QT-L4-01)**:
     - Gỡ bỏ giá trị cố định `2026-10-30` trên các template HTML, bổ sung `th:min="${#temporals.format(T(java.time.LocalDate).now(), 'yyyy-MM-dd')}"`.
     - Trong `app.js`, hàm `initializeAppointmentDateInputs()` tự động gán `min` là ngày hiện tại và chặn submit nếu ngày hẹn `< todayStr`.
     - Ở Backend: Thêm `@FutureOrPresent` vào `AssignTechnicianRequest.java` và kiểm tra trong `TicketController` / `TicketServiceImpl`: nếu `appointmentTime.isBefore(nowVietnam)` thì ném `AppException("INVALID_APPOINTMENT_TIME", "Thời gian hẹn giao – nhận máy phải ở trong tương lai (QT-L4-01)")`.

4. **Bước 4: Xác nhận bằng test (Verify via Regression Test)**
   - Chạy toàn bộ kiểm thử tự động của dự án:
     ```powershell
     .\mvnw.cmd test
     ```
   - Kết quả: `Tests run: 81, Failures: 0, Errors: 0, Skipped: 0` -> **BUILD SUCCESS**.
   - Bổ sung các ca kiểm thử:
     - `TC08_SlaStatusClassificationTests`: Kiểm tra phân loại `QUA_HAN`, `SAP_QUA_HAN`, `CON_HAN`.
     - `AppointmentTimeValidationTests`: Kiểm tra chặn ngày hẹn trong quá khứ (`assignTechnician_pastAppointmentTime_throwsInvalidAppointmentTime`) và chấp nhận ngày hẹn tương lai (`assignTechnician_futureAppointmentTime_succeeds`).
     - `LoggingAndMaskingTest`: Kiểm tra serializer Jackson định dạng ISO 8601 kèm múi giờ `+07:00`.