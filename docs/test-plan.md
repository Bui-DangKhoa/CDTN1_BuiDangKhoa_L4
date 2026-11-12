# Test Plan — Luồng L4: Phân công KTV và Lịch hẹn

- **Dự án:** Smart CRM – Mekong Mobile
- **Track:** Software Engineering (SE)
- **Người thực hiện:** Bùi Đăng Khoa – 2374802013428
- **Ngày cập nhật:** 12/11/2026

## 1. Phạm vi kiểm thử và phần chủ ý không kiểm

### Trong phạm vi

- QT-04: Tính hạn cam kết theo mức ưu tiên, bỏ qua Chủ nhật.
- QT-06: Chuyển trạng thái đúng vòng đời và ghi nhật ký.
- QT-07: Đổi KTV phải có lý do tối thiểu 10 ký tự.
- QT-08: Phân công KTV đủ điều kiện về tay nghề và trung tâm.
- QT-13: Xóa mềm phiếu và giữ lịch sử.
- QT-15: Che số điện thoại trong dữ liệu trả cho KTV.
- QT-L4-01: Từ chối lịch hẹn xung đột.
- Kiểm soát chủ sở hữu: KTV không được cập nhật phiếu của KTV khác.
- Tính tái lập: Dựng trong Compose project/volume/ports cô lập, kiểm tra dữ liệu mẫu, chạy lại schema/seed và chạy lại bộ kiểm thử.

### Ngoài phạm vi

- Gửi SMS/Zalo: chưa được hiện thực trong phạm vi prototype.
- Hiệu năng, tải lớn và kiểm thử xâm nhập: ngoài phạm vi BT3.
- Các màn hình chỉ đọc không liên quan đến quy tắc nghiệp vụ được lựa chọn.

## 2. Loại kiểm thử và số lượng

Bộ kiểm thử tự động gần nhất có 108 test, sử dụng JUnit 5, Mockito và H2 theo cấu hình của từng lớp.

Test Report chọn 12 tình huống đại diện:

| Tầng/cách kiểm thử | Số tình huống | Mã |
|---|---:|---|
| Unit test service | 8 | TC-01, TC-02, TC-03, TC-04, TC-06, TC-07, TC-08, TC-10 |
| MockMvc với service mock | 1 | TC-05 |
| Kiểm thử UI với MySQL | 1 | TC-09 |
| Kiểm thử REST với MySQL | 1 | TC-11 |
| Kiểm tra Docker và chạy lại kiểm thử | 1 | TC-12 |
| Tổng | 12 | |

Các unit test trong Test Report là tình huống được chọn từ bộ kiểm thử tự động; không cộng thêm vào tổng 108 test.

Kỹ thuật chọn dữ liệu:

- Phân vùng tương đương: dữ liệu hợp lệ và không hợp lệ.
- Giá trị biên: lý do đổi KTV dài 9 và 10 ký tự.
- Chuyển trạng thái: chuyển hợp lệ và chuyển bỏ bước.
- Kiểm thử bảo mật: dùng tài khoản KTV để cập nhật phiếu của KTV khác.
- Kiểm thử tái lập: chạy lại trên cùng mã nguồn và cùng điều kiện dữ liệu thử.

## 3. Tiêu chí thành công đo được

- 12/12 tình huống có kết quả thực tế và minh chứng sau khi thực thi.
- Mỗi quy tắc trong phạm vi có ít nhất một test truy vết.
- Có ít nhất hai test giá trị biên ở hai phía của cùng ranh giới.
- Có ít nhất hai test dữ liệu sai, một test bảo mật và một test tái lập.
- Bộ kiểm thử tự động có Failures = 0, Errors = 0, Skipped = 0.
- Không còn bug Critical đang mở; bug Major chưa sửa phải có lý do và hướng xử lý.
- Sau khi dựng sạch, app và MySQL đều healthy, đăng nhập được và dữ liệu mẫu được nạp đúng.

Kết quả tự động gần nhất đã ghi nhận:

108 test, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS trong builder container cùng host. Lệnh Windows Wrapper bị lỗi cache/Javac; xem `docs/test-report.md`.

Kết quả này không thay thế minh chứng riêng cho các tình huống kiểm tra thủ công.

## 4. Môi trường và dữ liệu thử

### Môi trường

- Windows và PowerShell.
- Java 21, Spring Boot, Maven Wrapper.
- JUnit 5, Mockito và H2 theo cấu hình từng lớp test.
- Docker Compose, ứng dụng Spring Boot và MySQL 8.0.
- Trình duyệt và Swagger UI để kiểm tra API.

### Dữ liệu và cách làm sạch

- Unit test: khởi tạo fixture và mock theo từng test, không dùng dữ liệu MySQL đang chạy.
- Test dùng H2: sử dụng cấu hình test của dự án; phải bảo đảm dữ liệu được khởi tạo và cô lập giữa các lần chạy.
- Kiểm tra trên Docker: volume mới, schema và hai seed SQL; lần chạy C8 lặp lại schema hai lần và từng seed một lần trong DB cô lập.
- Tài khoản mẫu: quanly1 và ktv1.
- TC-09: phiếu thử 102 và lịch thử; ghi trạng thái, technician_id, số lịch hẹn và log trước/sau thao tác.
- TC-12: kiểm tra trên môi trường cô lập, volume riêng và host ports riêng. Không xóa volume của môi trường đang dùng.
- C8 import lần hai: counts trước/sau là 11/4/2; cả hai schema run và hai seed reimport exit 0.

### Lệnh thực thi

Chạy toàn bộ kiểm thử trong PowerShell:

    .\mvnw.cmd clean test "-DforkCount=0"

Chạy một phương thức test:

    .\mvnw.cmd "-Dtest=TicketServiceTest#<ten_phuong_thuc>" test

Dựng hệ thống:

    docker compose up -d --build

Kiểm tra trạng thái:

    docker compose ps

Không chạy lệnh xóa volume trên dự án đang dùng. Dựng và dọn môi trường TC-12 chỉ trong compose project cô lập do lần kiểm thử tạo ra.
