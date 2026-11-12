# Bug Log — Luồng L4: Phân công kỹ thuật viên và lịch hẹn

- **Hệ thống:** Smart CRM – Mekong Mobile
- **Sinh viên:** Bùi Đăng Khoa — MSSV: 2374802013428
- **Công nghệ:** Java 21, Spring Boot 3.3.4, Thymeleaf, MySQL 8.0, Docker
- **Môi trường kiểm thử:** Windows, PowerShell, Maven Wrapper, Docker Compose
- **Ngày tổng hợp:** 12/11/2026
- **Phạm vi báo cáo:** 3 bug đã sửa và kiểm thử lại.

## Tổng hợp

| Mã bug | Nội dung | Mức độ | Trạng thái | Commit sửa |
|---|---|---|---|---|
| BUG-01 | Truyền technician ID thay cho user ID khi ghi nhận người đổi trạng thái | Major | Đã sửa, kiểm thử lại đạt | `08e1d19` |
| BUG-02 | Cho phép phân công kèm lịch hẹn trong quá khứ | Major | Đã sửa, kiểm thử lại đạt | `d4fb3d1` |
| BUG-03 | Giờ lịch hẹn trên giao diện lệch 7 giờ so với MySQL | Major | Đã sửa, kiểm thử lại đạt | `d24247e`, `ff8f3cf` |

## BUG-01 — Sai actor khi kỹ thuật viên cập nhật trạng thái phiếu

### 1. Mức độ và tác động

**Major — Sai thông tin người thực hiện trong lịch sử thao tác.**

Khi kỹ thuật viên cập nhật trạng thái phiếu, controller truyền ID kỹ thuật viên thay cho ID tài khoản đăng nhập vào trường `changedBy`. Nếu hai ID khác nhau, dữ liệu audit có thể ghi sai người thực hiện, làm giảm độ tin cậy của lịch sử xử lý phiếu.

### 2. Phát hiện bởi

Kiểm thử MockMvc:

`ThymeleafViewsTest#testUpdateStatus_ChangedByUsesUserIdNotTechnicianId`

Test chủ động thiết lập hai giá trị khác nhau:

- User ID của tài khoản đăng nhập: `2`.
- Technician ID được ánh xạ từ tài khoản: `1`.

### 3. Triệu chứng trước sửa

Giá trị actor mong đợi là user ID `2`, nhưng controller truyền technician ID `1`.

```text
expected: <2> but was: <1>
```

### 4. Cách tái lập

**Tiền điều kiện:**

- Tài khoản `ktv1` có user ID `2`.
- Kỹ thuật viên tương ứng có technician ID `1`.
- Test mock các điều kiện cần thiết để cho phép cập nhật trạng thái.

**Các bước:**

1. Tạo session đăng nhập chứa user ID `2`.
2. Mock kết quả ánh xạ tài khoản sang kỹ thuật viên ID `1`.
3. Gửi POST `/technician/tickets/106/status` với `nextStatus=DANG_XU_LY`.
4. Dùng `ArgumentCaptor<UpdateTicketStatusDto>` bắt DTO controller truyền vào service.
5. Kiểm tra `changedBy` phải bằng `2`.

**Lệnh chạy trên PowerShell:**

```powershell
.\mvnw.cmd "-Dtest=ThymeleafViewsTest#testUpdateStatus_ChangedByUsesUserIdNotTechnicianId" test
```

### 5. Nguyên nhân gốc

Trong `TechnicianWebController.java`, actor được lấy bằng:

```java
Long changedById = techOpt.get().getTechnicianId();
```

Giá trị này thuộc bảng kỹ thuật viên, trong khi actor của thao tác phải là ID tài khoản đang đăng nhập.

### 6. Cách sửa và kết quả kiểm thử lại

Thay cách lấy actor bằng:

```java
Long changedById = currentUser.getUserId();
```

Sau sửa, test xác nhận controller truyền user ID `2` vào service dù technician ID là `1`.

**Kết quả kiểm thử lại:**

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Bằng chứng:**

- `docs/screenshots/TC-05-audit-pass.png`
- `docs/screenshots/TC-05-audit-assertions.png`

**Giới hạn kiểm chứng:** Đây là test MockMvc với service được mock, xác nhận actor được truyền đúng từ controller. Test này chưa xác nhận trực tiếp bản ghi `ticket_status_log` trên MySQL thật.

### 7. Commit sửa

[08e1d19 — fix(audit): ghi changed_by theo user_id người đăng nhập](https://github.com/Bui-DangKhoa/CDTN1_BuiDangKhoa_L4/commit/08e1d19ee04997ace4eed5c1da419e6ce3d120e4)

---

## BUG-02 — Cho phép đặt lịch hẹn trong quá khứ

### 1. Mức độ và tác động

**Major — Chấp nhận thời gian hẹn không hợp lệ.**

Luồng phân công kỹ thuật viên kèm lịch hẹn có thể chấp nhận thời điểm đã qua. Lịch hẹn được tạo không còn ý nghĩa thực tế và có thể gây sai lệch trong quá trình theo dõi, giao nhận thiết bị.

### 2. Phát hiện bởi

Kiểm thử service:

`TicketServiceTest.AppointmentTimeValidationTests.assignTechnician_pastAppointmentTime_throwsInvalidAppointmentTime`

### 3. Triệu chứng trước sửa

Test mong đợi service ném ngoại lệ khi nhận thời gian hẹn trong quá khứ, nhưng không có ngoại lệ được ném.

```text
Expected ... to be thrown, but nothing was thrown.
Tests run: 23, Failures: 1, Errors: 0, Skipped: 0
```

### 4. Cách tái lập

**Tiền điều kiện:**

- Có phiếu ở trạng thái cho phép phân công.
- Kỹ thuật viên được chọn đủ điều kiện xử lý phiếu.
- Các mock cần thiết đã được thiết lập để luồng phân công đi đến bước kiểm tra lịch hẹn.

**Các bước:**

1. Chuẩn bị `appointmentTime` nằm trong quá khứ so với giờ Việt Nam.
2. Gọi `assignTechnician(...)` kèm thời gian đó.
3. Kiểm tra service có từ chối bằng mã `INVALID_APPOINTMENT_TIME` hay không.

**Lệnh chạy nhóm test trên PowerShell:**

```powershell
.\mvnw.cmd '-Dtest=TicketServiceTest$AppointmentTimeValidationTests' test
```

### 5. Nguyên nhân gốc

Luồng phân công kèm lịch hẹn chưa kiểm tra `appointmentTime` có nằm trong quá khứ hay không.

Ngoài ra, các template phân công còn gán cứng ngày mặc định:

```html
value="2026-10-30"
```

Ngày mặc định này có thể trở thành ngày quá khứ khi ứng dụng tiếp tục được sử dụng.

### 6. Cách sửa và kết quả kiểm thử lại

Các thay đổi trong commit sửa:

- Bổ sung kiểm tra thời gian tại controller và service theo múi giờ `Asia/Ho_Chi_Minh`.
- Ném `AppException` mã `INVALID_APPOINTMENT_TIME` nếu thời điểm hẹn đã qua.
- Thêm `@FutureOrPresent` cho trường ngày hẹn trong DTO.
- Bỏ ngày mặc định gán cứng trong template.
- Bổ sung thuộc tính ngày tối thiểu và kiểm tra JavaScript trên form phân công.

Sau sửa, test thời gian quá khứ chạy đạt. Bộ test cuối cùng cũng chạy thành công:

```text
Tests run: 108, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Giới hạn kiểm chứng:** Kết quả trên xác nhận việc từ chối thời gian hẹn trong quá khứ. Chưa có bằng chứng đếm bản ghi MySQL trước/sau riêng cho trường hợp này; không kết luận rằng repository chưa từng được gọi `save` trước khi ngoại lệ xảy ra.

### 7. Commit sửa

[d4fb3d1 — fix(service): Ngăn đặt lịch hẹn trong quá khứ](https://github.com/Bui-DangKhoa/CDTN1_BuiDangKhoa_L4/commit/d4fb3d11262137d7aca0b2a91bda8c51266586cd)

**Lưu ý truy vết:** Commit và thông báo trong mã nguồn sử dụng nhãn `QT-L4-01`. Nội dung bug trong báo cáo được xác định theo hành vi “cho phép đặt lịch trong quá khứ”, không nhầm với trường hợp chống trùng lịch của TC-09.

---

## BUG-03 — Giờ lịch hẹn trên giao diện lệch 7 giờ so với MySQL

### 1. Mức độ và tác động

**Major — Hiển thị sai thời gian hẹn cho khách hàng.**

Giờ lịch hẹn trên trang tra cứu không khớp dữ liệu MySQL, có độ lệch 7 giờ. Khách hàng có thể hiểu sai giờ giao nhận thiết bị, ảnh hưởng đến việc phối hợp với kỹ thuật viên và trung tâm.

### 2. Phát hiện bởi

Kiểm thử thủ công trên Docker, đối chiếu:

- Thời gian nhập trên form phân công.
- Thông báo đặt lịch thành công.
- Cột `appointment_time` trong MySQL.
- Thời gian trên trang tra cứu khách hàng.
- Kết quả sau khi khởi động lại ứng dụng.

### 3. Triệu chứng trước sửa

Các mẫu dữ liệu đã quan sát:

| Phiếu | Thời gian trong MySQL | Thời gian trên giao diện | Sai lệch |
|---|---|---|---|
| `TK-202610-002` | 15/11/2026 15:00 | 15/11/2026 08:00 | Sớm 7 giờ |
| `TK-202610-003` | 27/10/2026 09:30 | 27/10/2026 02:30 | Sớm 7 giờ |

### 4. Cách tái lập

**Tiền điều kiện:** Ứng dụng và MySQL chạy trên Docker với phiên bản trước sửa.

**Các bước:**

1. Đăng nhập bằng tài khoản quản lý.
2. Phân công kỹ thuật viên và đặt một lịch hẹn.
3. Ghi lại thời gian nhập và thông báo thành công.
4. Truy vấn thời gian lịch hẹn trong MySQL:

```powershell
docker exec -it smartcrm-mysql mysql -ucrm_user -p smart_crm_db -e "SELECT a.appointment_id, t.ticket_code, a.technician_id, a.appointment_time FROM appointment a JOIN ticket t ON t.ticket_id=a.ticket_id ORDER BY a.appointment_id DESC LIMIT 10;"
```

5. Mở trang `/lookup`, tra cứu mã phiếu tương ứng.
6. So sánh giờ trên giao diện với `appointment_time` trong MySQL.
7. Khởi động lại ứng dụng và kiểm tra lại trang tra cứu.

### 5. Nguyên nhân gốc

Cấu hình múi giờ chưa được đồng bộ xuyên suốt giữa ứng dụng và môi trường Docker; cách định dạng thời gian JSON chưa được quy định thống nhất.

Bằng chứng trước sửa ghi nhận độ lệch 7 giờ giữa MySQL và giao diện. Chưa có kiểm thử cô lập để xác định riêng thành phần gây chuyển đổi sai.

### 6. Cách sửa và kết quả kiểm thử lại

**Các thay đổi:**

- Đồng bộ múi giờ `Asia/Ho_Chi_Minh` cho container và JVM.
- Cấu hình MySQL dùng múi giờ `+07:00`.
- Đồng bộ cấu hình Jackson và Hibernate JDBC.
- Bổ sung `JacksonConfig` để xuất `LocalDateTime` kèm offset `+07:00`.
- Chuyển dữ liệu đầu vào có offset về giờ Việt Nam.
- Đồng bộ thời điểm tạo entity, audit và response theo giờ Việt Nam.

**Dữ liệu kiểm thử lại:**

- Phiếu: `TK-202610-001`.
- Lịch hẹn mới: ngày `16/11/2026`, lúc `08:00`.

**Kết quả thực tế:**

| Điểm kiểm tra | Thời gian quan sát |
|---|---|
| Form đặt lịch | 16/11/2026 08:00 |
| Thông báo thành công | 16/11/2026 08:00 |
| MySQL | 16/11/2026 08:00 |
| Trang tra cứu | 16/11/2026 08:00 |
| Trang tra cứu sau restart | 16/11/2026 08:00 |

**Kết luận:** PASS — thời gian nhất quán trong luồng đã kiểm thử, kể cả sau khi khởi động lại ứng dụng.

**Bằng chứng:**

- `docs/screenshots/BUG-03-before-db.png`
- `docs/screenshots/BUG-03-before-lookup-002.png`
- `docs/screenshots/BUG-03-before-lookup-003.png`
- `docs/screenshots/BUG-03-retest-input.png`
- `docs/screenshots/BUG-03-retest-success.png`
- `docs/screenshots/BUG-03-retest-db.png`
- `docs/screenshots/BUG-03-retest-lookup.png`
- `docs/screenshots/BUG-03-retest-restart.png`
- `docs/screenshots/BUG-03-retest-after-restart.png`

**Giới hạn kiểm chứng:** Kiểm thử thủ công xác nhận luồng giao diện/MySQL nêu trên. Chưa kiểm thử riêng toàn bộ định dạng thời gian JSON mà cấu hình mới hỗ trợ.

### 7. Commit sửa

- [d24247e — Đồng bộ múi giờ JVM, container và MySQL](https://github.com/Bui-DangKhoa/CDTN1_BuiDangKhoa_L4/commit/d24247edffa39ae416304323b014be4d5dfe6d88).
- [ff8f3cf — Bổ sung cấu hình Jackson và thời gian trong ứng dụng](https://github.com/Bui-DangKhoa/CDTN1_BuiDangKhoa_L4/commit/ff8f3cf1f22a48ca2e469a6065084862168c5abf).

## Kết quả hồi quy cuối cùng

Lệnh mục tiêu trên Windows PowerShell:

```powershell
.\mvnw.cmd clean test "-DforkCount=0"
```

Lần chạy mới ngày 12/11/2026 được thực hiện trong builder container tạm bằng Maven 3.9.16/Java 21.0.12 sau khi lệnh Windows không chạy do lỗi cache/Javac:

```text
Tests run: 108, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Thời gian Maven báo: 21.573 giây. Windows Wrapper lần chạy này không tới giai đoạn test; xem chi tiết lỗi môi trường trong `docs/test-report.md`.

Ba bug trong phạm vi báo cáo đã được sửa và kiểm thử lại theo các bằng chứng nêu trên. Kết quả chạy toàn bộ test được sử dụng làm bằng chứng hồi quy, không thay thế các kiểm tra thủ công trên Docker/MySQL.
