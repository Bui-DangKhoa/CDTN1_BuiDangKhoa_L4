# Test Report — Luồng L4: Phân công kỹ thuật viên và lịch hẹn (track SE)

- **Hệ thống:** Smart CRM – Mekong Mobile · Java 21, Spring Boot 3.3.4, MySQL 8.0
- **Môi trường chạy:** Windows, PowerShell, Maven Wrapper; test API/integration chạy trên container docker-compose
- **Ngày chạy:** 12/11/2026
- **Lệnh chạy toàn bộ:** `./mvnw test`
- **Lệnh chạy một test:** `./mvnw test -Dtest=TicketServiceTest#<tên_phương_thức>`
- **Ảnh chụp:** `docs/screenshots/TC-xx.png` (FAIL và sau khi sửa lưu riêng: `TC-xx-fail.png`, `TC-xx-pass.png`)

## Tổng hợp

| TC | Loại | Quy tắc | Tầng | Kết quả | Bug |
|---|---|---|---|---|---|
| TC-01 | Luồng đúng | QT-08 | Unit | _chưa chạy_ | |
| TC-02 | Biên | QT-07 | Unit | _chưa chạy_ | |
| TC-03 | Biên | QT-07 | Unit | _chưa chạy_ | |
| TC-04 | Luồng đúng | QT-04 | Unit | **FAIL** → _PASS sau sửa (điền sau)_ | BUG-02 |
| TC-05 | Luồng đúng | QT-06 | Unit | _chưa chạy_ | |
| TC-06 | Luồng đúng | QT-13 | Unit | _chưa chạy_ | |
| TC-07 | Luồng đúng | QT-15 | Unit | _chưa chạy_ | |
| TC-08 | Dữ liệu sai | QT-06 | Unit | _chưa chạy_ | |
| TC-09 | Dữ liệu sai | QT-L4-01 | Integration | _chưa chạy_ | |
| TC-10 | Dữ liệu sai | QT-08 | Integration | _chưa chạy_ | |
| TC-11 | Bảo mật | A01 / QT-15 | API | _chưa chạy_ | |
| TC-12 | Tái lập | C8 | Integration | _chưa chạy_ | |

## TC-01 — QT-08: phân công KTV đủ điều kiện

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** Phiếu id=10 trạng thái MOI, chưa có KTV, centerId=1, nhóm sự cố categoryId=1. KTV id=5 cùng trung tâm, nằm trong danh sách `findEligibleTechnicians(1, 1)` (mock).
- **Các bước:** 1. Gọi `assignTechnician(10, 5, 101, "Phân công KTV xử lý sự cố nguồn")`.
- **Dữ liệu thử:** ticketId=10, technicianId=5, assignedBy=101.
- **Kết quả mong đợi:** `status = DA_PHAN_CONG`, `technician.technicianId = 5`; `ticketRepository.save` và `statusLogRepository.save` mỗi hàm gọi đúng 1 lần.
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Pass** · Ảnh: `TC-01.png`

## TC-02 — QT-07 biên: lý do đổi KTV đúng 10 ký tự

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** Phiếu id=10 đã gán KTV id=5; KTV mới id=6 cùng trung tâm, đủ điều kiện QT-08.
- **Các bước:** 1. Gọi `assignTechnician(10, 6, 101, "Doi KTV 10")`.
- **Dữ liệu thử:** `changeReason = "Doi KTV 10"` (10 ký tự).
- **Kết quả mong đợi:** Không ném ngoại lệ; `technician.technicianId = 6`; `status = DA_PHAN_CONG`.
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Pass** · Ảnh: `TC-02.png`

## TC-03 — QT-07 biên: lý do đổi KTV 9 ký tự

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** Giống TC-02 (cùng ranh giới 9/10 ký tự).
- **Các bước:** 1. Gọi `assignTechnician(10, 6, 101, "Doi KTV m")`.
- **Dữ liệu thử:** `changeReason = "Doi KTV m"` (9 ký tự).
- **Kết quả mong đợi:** Ném `AppException` mã `INVALID_CHANGE_REASON`, thông báo chứa "từ 10 ký tự trở lên"; **không** có `save` nào được gọi như một thay đổi hợp lệ cho phiếu (kiểm tác dụng phụ).
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Pass** · Ảnh: `TC-03.png`

## TC-04 — QT-04: hạn cam kết bỏ qua Chủ nhật

- **Mức ưu tiên:** Cao (quy tắc nghiệp vụ cốt lõi)
- **Tiền điều kiện:** Không cần CSDL; `receivedAt` được truyền trực tiếp nên kết quả không phụ thuộc ngày chạy.
- **Các bước:**
  1. Mở terminal tại thư mục gốc dự án.
  2. Chạy `./mvnw test -Dtest=TicketServiceTest#TC04_dueDate_skipsSunday`
- **Dữ liệu thử:** `receivedAt = 2026-11-07T15:00` (thứ Bảy), `priority = "CAO"` (24 giờ làm việc).
- **Kết quả mong đợi:** `due_date = 2026-11-09T15:00` (thứ Hai), vì QT-04 chỉ tính thứ Hai đến thứ Bảy nên Chủ nhật 08/11 bị bỏ qua.
- **Kết quả thực tế (lần 1, 12/11/2026 08:26):** Test trả `expected: <2026-11-09T15:00> but was: <2026-11-08T15:00>`; `Tests run: 1, Failures: 1, Errors: 0`; BUILD FAILURE. Hạn cam kết rơi đúng vào Chủ nhật.
- **PASS / FAIL:** **FAIL** → ghi BUG-02 (xem `docs/bug-log.md`). Ảnh: `docs/screenshots/TC-04-fail.png`
- **Chạy lại sau khi sửa (commit `<mã>`):** _điền sau_ · Ảnh: `docs/screenshots/TC-04-pass.png`

## TC-05 — QT-06: chuyển trạng thái hợp lệ và ghi log

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** Phiếu id=10 trạng thái DA_PHAN_CONG.
- **Các bước:** 1. Gọi `updateTicketStatus(10, newStatus=DANG_XU_LY, changedBy=2)`.
- **Dữ liệu thử:** ticketId=10, newStatus=DANG_XU_LY, changedBy=2.
- **Kết quả mong đợi:** `status = DANG_XU_LY`; `ticket_status_log` có 1 dòng mới `from=DA_PHAN_CONG, to=DANG_XU_LY, changed_by=2`.
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Fail** · Ảnh: `TC-05.png`

## TC-06 — QT-13: xóa mềm phiếu ở trạng thái MOI

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** Phiếu id=10 trạng thái MOI, `deleted_at = NULL`, không có lịch hẹn còn hiệu lực.
- **Các bước:** 1. Gọi `deleteTicket(10, 1)`.
- **Dữ liệu thử:** ticketId=10, changedBy=1.
- **Kết quả mong đợi:** `deleted_at` có giá trị, `status` vẫn MOI; log có dòng `from=MOI, to=DA_XOA`; dòng phiếu **không** bị xóa vật lý.
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Pass** · Ảnh: `TC-06.png`

## TC-07 — QT-15: che 4 số giữa SĐT cho KTV

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** Phiếu gán cho KTV id=5, SĐT khách trong dữ liệu là `0901234567`.
- **Các bước:** 1. Gọi `getTicketsForTechnician(5)`.
- **Dữ liệu thử:** technicianId=5.
- **Kết quả mong đợi:** `customerPhone = "090****567"`, không xuất hiện số gốc.
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Pass** · Ảnh: `TC-07.png`

## TC-08 — QT-06 dữ liệu sai: nhảy cóc trạng thái

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** Phiếu id=10 trạng thái DA_PHAN_CONG.
- **Các bước:** 1. Gọi `updateTicketStatus(10, newStatus=HOAN_TAT, changedBy=2)`.
- **Dữ liệu thử:** ticketId=10, newStatus=HOAN_TAT.
- **Kết quả mong đợi:** Bị từ chối (ngoại lệ); `status` vẫn DA_PHAN_CONG; không `save`, không ghi log.
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Fail** · Ảnh: `TC-08.png`

## TC-09 — QT-L4-01 dữ liệu sai: lịch hẹn trùng, không để lại dữ liệu hỏng

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** KTV A đã có lịch lúc 14:00; ghi trước số dòng `appointment` (N) và trạng thái phiếu cần gán.
- **Các bước:** 1. Gán phiếu mới MOI cho KTV A kèm giờ hẹn 14:15.
- **Dữ liệu thử:** appointmentTime = 14:15 cùng ngày (trong vùng ±30 phút).
- **Kết quả mong đợi:** Lỗi `SCHEDULE_CONFLICT` kèm mã HTTP và thông báo; sau lỗi, phiếu vẫn MOI, `technician_id = NULL`, số dòng `appointment` vẫn N, `ticket_status_log` không tăng.
- **Kết quả thực tế:** _điền sau (ghi số trước/sau)_
- **PASS / FAIL:** **Fail** · Ảnh: `TC-09.png`

## TC-10 — QT-08 dữ liệu sai: KTV không đủ điều kiện (truy vấn thật)

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** CSDL H2/MySQL có KTV: proficiency=2, proficiency=3, KTV khác trung tâm, KTV không active.
- **Các bước:** 1. Gọi `findEligibleTechnicians(centerId, categoryId)`.
- **Mong đợi:** Chỉ trả KTV proficiency ≥ 3, cùng trung tâm, đang hoạt động.
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Pass** · Ảnh: `TC-10.png`

## TC-11 — Bảo mật (A01 / QT-15)

- **Mức ưu tiên:** Cao
- **Tiền điều kiện:** Docker chạy; đăng nhập `ktv1`; có phiếu gán cho KTV khác.
- **Các bước:** _điền sau khi quyết định phiên bản test cuối cùng_
- **Mong đợi:** _điền sau_
- **Kết quả thực tế:** _điền sau khi chạy_
- **PASS / FAIL:** **Fail** · Ảnh: `TC-11.png`

## TC-12 — Tái lập: nạp seed lần hai

- **Mức ưu tiên:** Trung bình
- **Tiền điều kiện:** `docker compose up -d --build` xong, mọi dịch vụ healthy.
- **Các bước:** 1. Đếm số dòng `ticket`, `technician`, `app_user`. 2. `docker compose restart app`. 3. Đếm lại.
- **Mong đợi:** Số dòng không đổi (BT2 ghi 9, 4, 2).
- **Kết quả thực tế:** _điền sau khi chạy (trước → sau)_
- **PASS / FAIL:** **Pass** · Ảnh: `TC-12.png`