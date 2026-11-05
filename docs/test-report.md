# Test Report — Luồng L4

| Mã | Tiền điều kiện | Các bước | Dữ liệu thử | Kết quả mong đợi | Ưu tiên | Kết quả thực tế | PASS/FAIL |
|---|---|---|---|---|---|---|---|
| TC-01 (QT-08, luồng đúng) | Phiếu ở trạng thái MOI; KTV cùng trung tâm, proficiency >= 3 | 1. Đăng nhập quanly1 2. Mở /tickets/unassigned 3. Chọn phiếu, chọn KTV, bấm "Xác nhận gán" | ticket: <mã phiếu>; KTV: <tên> | Phiếu chuyển DA_PHAN_CONG, technician_id đúng; ticket_status_log tăng đúng 1 dòng | Cao | Thông báo “Phân công kỹ thuật viên Lê Văn An cho phiếu #TK-202610-001 thành công!”; ticket 101: DA_PHAN_CONG, technician_id = 2; ticket_status_log 2 → 3, dòng mới MOI → DA_PHAN_CONG, changed_by = 1; bảng appointment vẫn 6 dòng (không tạo lịch hẹn khi chưa chọn khung giờ). | PASS · docs/screenshots/TC-01.png |
