# Bảng công khai việc sử dụng AI (AI Disclosure Statement)

## 1. Thông tin sinh viên & Học phần
- **Sinh viên thực hiện**: Bùi Đăng Khoa
- **Mã số sinh viên (MSSV)**: 2374802013428 – **Lớp**: L4-SE
- **Học phần**: Chuyên đề tốt nghiệp 1 (Học kỳ 1, Năm học 2026 – 2027)
- **Đề tài**: Hệ thống Mekong Mobile Smart CRM – Luồng L4 (Phân công Kỹ thuật viên & Lịch hẹn)

---

## 2. Bảng kê khai chi tiết việc sử dụng công cụ AI

| STT | Công cụ AI | Mục đích sử dụng | Áp dụng ở thành phần nào | Phương pháp kiểm chứng mã nguồn của sinh viên |
| :---: | :--- | :--- | :--- | :--- |
| **1** | **Google Antigravity** *(Advanced Agentic AI)* | Hỗ trợ scaffold khung mã nguồn Spring Boot 3 theo kiến trúc 3 lớp; gợi ý cấu trúc các DTO Request/Response và Exception Handler. | `src/main/java/` (`controllers`, `services`, `repositories`, `dto`) | Sinh viên trực tiếp rà soát từng lớp; cấu hình tường minh mối quan hệ JPA Entity; thiết lập các quy tắc Bean Validation (`@Valid`, `@NotNull`, `@Min`) và đối chiếu với sơ đồ kiến trúc `architecture.drawio`. |
| **2** | **Google Antigravity** | Hỗ trợ sinh khung mã kiểm thử Unit Test cho Tầng Service (JUnit 5 & Mockito) theo khuôn Given-When-Then. | `src/test/java/` (`TicketServiceTest`, `AppointmentServiceTest`, `AuthServiceTest`, `AuthAndSecurityTest`) | Sinh viên tự tay điều chỉnh dữ liệu đầu vào (mock data); bổ sung các trường hợp biên (edge cases) như 9 ký tự vs 10 ký tự cho QT-07, KTV bậc 2 vs bậc 3 cho QT-08; chạy `./mvnw test` đạt kết quả PASS 100% (25/25 tests). |
| **3** | **Google Antigravity** | Hỗ trợ chuyển đổi bản vẽ Wireframe thành giao diện Thymeleaf HTML5 và CSS đồng bộ chuẩn màu `#ffffff` và thanh điều hướng `#1976D2`. | `src/main/resources/templates/` & `static/css/style.css` | Mở trực tiếp trên trình duyệt ở độ phân giải 1280px; kiểm tra từng trạng thái (Loading spinner, Empty state, Error alert); kiểm thử tương tác Modal căn giữa và validation chặn submit khi đổi KTV cũ. |
| **4** | **Google Antigravity** | Hỗ trợ viết Dockerfile multi-stage build và file cấu hình `docker-compose.yml` kết nối container ứng dụng với MySQL 8.0. | `Dockerfile`, `docker-compose.yml`, `.env.example` | Kiểm tra kích thước image sau build; kiểm thử script nạp CSDL `schema.sql` và `seeds/*.sql` đạt tiêu chí Idempotent C5 khi khởi động lại container hoặc chạy lại script migration/seed lần thứ 2 trên CSDL hiện hành (giữ nguyên volume dữ liệu). |
| **5** | **Gemini Notebook** | Gợi ý cấu trúc bản SRS rút gọn và rà soát câu từ các User Story theo khuôn chuẩn INVEST. | `docs/srs.md` | Tự đọc lại từng User Story; tự điều chỉnh vế "để &lt;giá trị&gt;" gắn trực tiếp với nỗi đau V2, V3 trong case study Mekong Mobile; tự viết bổ sung 10 tiêu chí Given–When–Then cho luồng chính và luồng ngoại lệ. |
| **6** | **Gemini Notebook** | Gợi ý mã sơ đồ UML cho Use Case và Sơ đồ Kiến trúc phân lớp. | `docs/usecase.drawio`, `docs/architecture.drawio` | Tự sửa hướng mũi tên `<<extend>>`; chuẩn hóa thuật ngữ "phiếu bảo hành"; tự bổ sung khung Legend chú thích ký hiệu và vùng NGOÀI PHẠM VI theo quy định. |
| **7** | **Không dùng AI (Tự thực hiện 100%)** | Hiện thực hóa toàn bộ logic nghiệp vụ cốt lõi, kiểm soát vòng đời một chiều và xây dựng bộ kiểm thử đơn vị hồi quy. | `TicketServiceImpl.java`, `AppointmentService.java`, `TicketServiceTest.java`, `AppointmentServiceTest.java` | Tự viết và hoàn thiện 100% thuật ngữ, quy tắc nghiệp vụ (QT-04, QT-07, QT-08, QT-L4-01, QT-13, QT-14, QT-15); tự thiết kế các test case biên và ngoại lệ bao gồm hàm kiểm thử `testCreateAppointment_ScheduleConflict_QTL401`; chạy `./mvnw test` đạt kết quả PASS 100%. |

---

## 3. Lời cam kết Liêm chính Học thuật (Academic Integrity Pledge)

Tôi xin cam đoan các nội dung sau:
1. Toàn bộ các đoạn mã nguồn và tài liệu có sự hỗ trợ của công cụ AI đều được tôi **đọc hiểu 100%**, trực tiếp tinh chỉnh, cấu hình và kiểm chứng tính đúng đắn về mặt kỹ thuật trước khi đưa vào kho lưu trữ chính thức.
2. Hệ thống đã được kiểm thử tự động toàn diện bằng bộ công cụ kiểm thử tiêu chuẩn (`JUnit 5`, `Mockito`) và kiểm thử thực nghiệm trên môi trường container hóa (`Docker`), đảm bảo không có mã rác, không phát sinh lỗi bảo mật nghiêm trọng và đáp ứng trọn vẹn các yêu cầu nghiệp vụ của đề tài.
3. Tôi hoàn toàn chịu trách nhiệm trước Khoa Công nghệ Thông tin – Trường Đại học Văn Lang và Giảng viên hướng dẫn về tính trung thực, minh bạch của bảng kê khai này cũng như chất lượng của toàn bộ sản phẩm nộp Bài tập 2.

*Thành phố Hồ Chí Minh, ngày 22 tháng 10 năm 2026*  
**Sinh viên thực hiện**  
*(Đã ký và xác nhận)*  
**Bùi Đăng Khoa**
