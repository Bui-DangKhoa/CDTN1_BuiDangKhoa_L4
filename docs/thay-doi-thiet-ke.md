# Nhật ký thay đổi thiết kế (Design Change Log)

Tài liệu ghi nhận toàn bộ các quyết định điều chỉnh kỹ thuật, kiến trúc và mã nguồn so với phiên bản Bài tập 1 (BT1) nhằm hoàn thiện yêu cầu Bài tập 2 (BT2 - Luồng L4 Mekong Mobile Smart CRM).

---

## 1. Các thay đổi sau phản hồi Bài tập 1 (Buổi 8 / Sprint 1)

### TD-01: Bổ sung bảng device và cột full_name trong bảng technician
- **Thay đổi gì**: Bổ sung bảng `device` và cột `full_name` vào file `docs/schema.sql`, đổi khóa ngoại `ticket.device_id` trỏ trực tiếp tới `device(device_id)`.
- **Lý do**: Đồng bộ dữ liệu 100% giữa Giao diện (Wireframe) ↔ CSDL (ERD/DDL) ↔ Bảng truy vết 5.2 của SRS sau phản hồi của GVHD.
- **Ảnh hưởng tới thành phần nào**: `docs/schema.sql`, `docs/erd.drawio`, `src/main/java/com/mekongmobile/smartcrm/entities/Device.java`.

### TD-02: Bổ sung Chú thích ký hiệu (Legend) cho ERD
- **Thay đổi gì**: Bổ sung khung Legend chú thích ký pháp chân chim (Crow's Foot) ở góc sơ đồ dữ liệu.
- **Lý do**: Đảm bảo sơ đồ mô hình dữ liệu đọc được độc lập mà không cần tác giả giải thích thêm, đạt tiêu chí kỹ thuật quy định.
- **Ảnh hưởng tới thành phần nào**: `docs/erd.drawio`.

### TD-03: Chuẩn hóa Sơ đồ Use Case và Bảng đối chiếu 5.2
- **Thay đổi gì**: Điền rõ nhãn `UC3` và `UC7`, loại bỏ các mũi tên `<<extend>>` dùng chưa chuẩn trong sơ đồ UML; sửa dòng đối chiếu trong `docs/srs.md` từ `FR5 ↔ US6` thành `FR5 ↔ US5`.
- **Lý do**: Chuẩn hóa ký pháp UML và sửa lỗi đánh máy liên kết truy vết nghiệp vụ.
- **Ảnh hưởng tới thành phần nào**: `docs/usecase.drawio`, `docs/srs.md`.

### TD-04: Bổ sung các tệp còn thiếu vào GitHub Repository
- **Thay đổi gì**: Đưa đầy đủ tệp `docs/schema.sql` (MySQL DDL) và `docs/wireframe.drawio` vào thư mục tài liệu dự án trên GitHub.
- **Lý do**: Đảm bảo repository đầy đủ tài liệu phục vụ việc clone & kiểm chứng của giảng viên.
- **Ảnh hưởng tới thành phần nào**: Thư mục `docs/`.

---

## 2. Các thay đổi kỹ thuật nâng cấp trong Bài tập 2 (Buổi 9 – Buổi 12 / Sprint 2)

### TD-05: Bổ sung DTO Mapper che SĐT khách hàng trên Cổng tra cứu công khai (QT-15)
- **Thay đổi gì**: Bổ sung logic che 4 số giữa của SĐT khách hàng (ví dụ: `0901234567` &rarr; `090****567`) trong `AppointmentService` (qua `MaskingUtil`) khi trả dữ liệu ra Cổng tra cứu công khai (`/lookup`).
- **Lý do**: Tuân thủ quy định bảo vệ dữ liệu cá nhân khách hàng (Nghị định 13/2023/NĐ-CP) và hiện thực hóa quy tắc nghiệp vụ QT-15 trên giao diện tra cứu không cần đăng nhập.
- **Ảnh hưởng tới thành phần nào**: `PublicAppointmentController.java`, `AppointmentService.java`, `appointment-lookup.html`, `AuthAndSecurityTest.java`.

### TD-06: Chuyển ô changeReason thành input form động cho Quản lý (QT-07)
- **Thay đổi gì**: Chuyển ô nhập lý do thay đổi KTV từ văn bản cố định sang form động `<textarea name="changeReason" id="changeReason" class="form-control" rows="3" minlength="10" placeholder="Nhập lý do thay đổi KTV (tối thiểu 10 ký tự)..."></textarea>`, bổ sung validation Javascript chặn submit nếu đổi KTV cũ mà nhập dưới 10 ký tự.
- **Lý do**: Đáp ứng chính xác quy tắc nghiệp vụ QT-07 theo SRS và Wireframe MH2, đảm bảo Quản lý bắt buộc phải nhập lý do có ý nghĩa ($\ge$ 10 ký tự) khi thay đổi điều phối và lưu vết vào `ticket_status_log`.
- **Ảnh hưởng tới thành phần nào**: `unassigned-list.html`, `assign-modal.html`, `assign-form.html`, `app.js`, `TicketServiceImpl.java`, `ticket_status_log`.

### TD-07: Tinh chỉnh giao diện nền trắng chuẩn Wireframe và khắc phục font UTF-8
- **Thay đổi gì**: Đồng bộ màu nền trắng `#ffffff`, viền bảng và card `#e5e7eb` chuẩn Wireframe trên cả 3 màn hình (MH1, MH2, MH3); căn giữa modal phân công (`modal-dialog-centered modal-lg`); thêm thẻ `<meta charset="UTF-8">` vào toàn bộ file Thymeleaf HTML và bổ sung cấu hình UTF-8 cho Spring Boot (`server.servlet.encoding.charset=UTF-8`, `spring.thymeleaf.encoding=UTF-8`) và MySQL JDBC Connection URL (`useUnicode=true&characterEncoding=UTF-8`).
- **Lý do**: Khắc phục triệt để lỗi hiển thị tiếng Việt có dấu trên môi trường triển khai Docker/Linux và đồng bộ 100% nhận diện thiết kế với bản vẽ `docs/wireframe.drawio`.
- **Ảnh hưởng tới thành phần nào**: `style.css`, `application.yml`, `application-docker.yml`, `docker-compose.yml`, các tệp HTML trong `src/main/resources/templates/`.

### TD-08: Tối ưu script SQL đảm bảo tính Idempotent khi nạp lại CSDL (C5)
- **Thay đổi gì**: Sử dụng mệnh đề `CREATE TABLE IF NOT EXISTS`, `ON DUPLICATE KEY UPDATE` và `INSERT IGNORE INTO` cho các bảng danh mục và dữ liệu kiểm thử trong `docs/schema.sql` và `seeds/*.sql`.
- **Lý do**: Đảm bảo tiêu chí Idempotent C5 trong tiêu chuẩn chấm điểm, cho phép chạy lại lệnh migration/seed hoặc khởi động lại hệ thống lần thứ 2 trên CSDL hiện hành (giữ nguyên volume dữ liệu, không xóa volume) mà không xảy ra lỗi trùng lặp khóa chính, không dừng container MySQL và bảo toàn nguyên vẹn số lượng bản ghi gốc.
- **Ảnh hưởng tới thành phần nào**: `docs/schema.sql`, `seeds/data-demo.sql`, `seeds/auth_data.sql`, `docker-compose.yml`.

### TD-09: Bổ sung Logging SLF4J nghiệp vụ và làm sạch GlobalExceptionHandler
- **Thay đổi gì**: Thêm `@Slf4j` cho các lớp Service (`TicketServiceImpl`, `AppointmentService`, `AuthService`) và ghi log `INFO` khi thao tác thành công, `WARN` khi vi phạm quy tắc nghiệp vụ; cập nhật `GlobalExceptionHandler` bắt ngoại lệ tập trung và không để lộ chuỗi SQL hoặc stack trace ra ngoài client.
- **Lý do**: Nâng cao chất lượng Clean Code, năng lực giám sát vận hành (Observability) và bảo mật an toàn thông tin theo chuẩn SE.
- **Ảnh hưởng tới thành phần nào**: `TicketServiceImpl.java`, `AppointmentService.java`, `AuthService.java`, `GlobalExceptionHandler.java`.

### TD-10: Đồng bộ tên Repository chính thức `CDTN1_BuiDangKhoa_L4`
- **Thay đổi gì**: Đổi tên kho lưu trữ GitHub từ `smart-crm-technician-scheduling` sang `CDTN1_BuiDangKhoa_L4` (`https://github.com/Bui-DangKhoa/CDTN1_BuiDangKhoa_L4.git`); cập nhật đường dẫn và lệnh clone trong `README.md`, thẻ `<name>`/`<url>` trong `pom.xml`.
- **Lý do**: Tuân thủ quy ước đặt tên repository nộp bài của học phần Chuyên đề tốt nghiệp 1 (`CDTN1_<HoTen>_<MaLuong>`).
- **Ảnh hưởng tới thành phần nào**: `README.md`, `pom.xml`, `docs/thay-doi-thiet-ke.md`, cấu hình Git remote `origin`.

### TD-11: Bổ sung liên kết khóa ngoại giữa bảng technician và app_user
- **Thay đổi gì**: Thêm cột `user_id BIGINT NULL UNIQUE` và ràng buộc khóa ngoại trỏ tới `app_user(user_id)` vào bảng `technician` trong CSDL, đồng bộ Entity `Technician.java` và seed data `ktv1`.
- **Lý do**: Khắc phục triệt để lỗi phân định KTV đăng nhập theo chuỗi họ tên hoặc coi `user_id = technician_id`, loại bỏ hoàn toàn lối dự phòng `findById(1L)` và đảm bảo an toàn dữ liệu cá nhân theo US4/QT-14.
- **Ảnh hưởng tới thành phần nào**: `docs/schema.sql`, `seeds/data-demo.sql`, `Technician.java`, `TechnicianRepository.java`, `TechnicianServiceImpl.java`, `TechnicianWebController.java`.

### TD-12: Bổ sung cột resolution vào bảng ticket phục vụ lịch sử sửa chữa thiết bị
- **Thay đổi gì**: Thêm cột `resolution VARCHAR(500) NULL` vào bảng `ticket` trong CSDL, đồng bộ Entity `Ticket.java`, tạo `DeviceRepairHistoryDto`, cập nhật truy vấn `findRepairHistory` và seed dữ liệu mẫu.
- **Lý do**: Cung cấp dữ liệu thực tế cho Khung 3 "Lịch sử sửa chữa thiết bị" (US5/FR5) theo đúng Wireframe MH3, loại bỏ hoàn toàn dữ liệu ghi cứng mà không dùng sai ngữ nghĩa của `issue_desc`.
- **Ảnh hưởng tới thành phần nào**: `docs/schema.sql`, `seeds/data-demo.sql`, `Ticket.java`, `DeviceRepairHistoryDto.java`, `TicketRepository.java`, `TicketServiceImpl.java`, `TechnicianWebController.java`.

### TD-13: Bổ sung cột deleted_at cho bảng ticket hỗ trợ xóa mềm
- **Thay đổi gì**: Thêm cột `deleted_at DATETIME NULL DEFAULT NULL` và chỉ mục `idx_ticket_deleted` vào bảng `ticket` trong CSDL, đồng bộ Entity `Ticket.java` và seed dữ liệu demo.
- **Lý do**: Đáp ứng yêu cầu xóa mềm (Soft Delete) theo quy tắc QT-13 của SRS (nghiêm cấm xóa vật lý phiếu sửa chữa và lịch hẹn để bảo toàn dữ liệu nhật ký QT-06).
- **Ảnh hưởng tới thành phần nào**: `docs/schema.sql`, `seeds/data-demo.sql`, `Ticket.java`, `TicketRepository.java`, `TicketServiceImpl.java`.

### TD-14: Chuẩn hóa trạng thái xóa mềm ticket và bổ sung endpoint CRUD đầy đủ
- **Thay đổi gì**: Thêm các REST endpoint PUT/DELETE cho Ticket và Appointment, quy định `ticket.status` luôn giữ nguyên (`MOI`) khi xóa mềm mà không bao giờ gán thành `DA_XOA`, chỉ ghi nhận giá trị `to_status = 'DA_XOA'` vào bảng `ticket_status_log` phục vụ kiểm toán QT-06/QT-13.
- **Lý do**: Đảm bảo toàn vẹn dữ liệu vòng đời đơn hướng theo QT-06, tuân thủ nguyên tắc không xóa vật lý của QT-13 và hoàn thiện đầy đủ 4 thao tác CRUD (chuẩn chấm điểm S1) với kiểm soát lỗi chặt chẽ (404/409/422).
- **Ảnh hưởng tới thành phần nào**: `TicketRestController.java`, `AppointmentRestController.java`, `TicketServiceImpl.java`, `AppointmentServiceImpl.java`, `ticket_status_log`.
