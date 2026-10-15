# Smart CRM – Hệ thống Quản lý Phân công Kỹ thuật viên & Lịch hẹn (Luồng L4)

## 1. Giới thiệu hệ thống
Hệ thống Smart CRM hỗ trợ số hóa và tự động hóa quy trình phân công kỹ thuật viên (KTV) và đặt lịch hẹn giao nhận máy cho trung tâm bảo hành Mekong Mobile. Hệ thống giải quyết tình trạng gán phiếu thủ công, lệch tải công việc giữa các KTV, đảm bảo tuân thủ thời hạn cam kết dịch vụ (SLA) và chống gán trùng lịch hẹn.

- **Trường**: Đại học Văn Lang – Khoa Công nghệ Thông tin
- **Học phần**: Chuyên đề tốt nghiệp 1 (HK1, 2026 – 2027)
- **Mã luồng nghiệp vụ**: L4 (Phân công KTV & Lịch hẹn)
- **Track chuyên ngành**: Software Engineering (SE)
- **GVHD**: TS. Nguyễn Trí Hải
- **SVTH**: Bùi Đăng Khoa – **MSSV**: 2374802013428 – **Lớp**: L4-SE
- **Repository**: https://github.com/Bui-DangKhoa/smart-crm-technician-scheduling.git

---

## 2. Yêu cầu môi trường & Danh mục công nghệ
- **Ngôn ngữ / Runtime**: Java 21 LTS (JDK 21)
- **Framework API**: Spring Boot 3 (v3.2+)
- **Truy cập dữ liệu**: Spring Data JPA (Hibernate ORM)
- **Cơ sở dữ liệu**: MySQL 8.0+ (Storage Engine: InnoDB)
- **Giao diện**: Thymeleaf
- **Kiểm thử**: JUnit 5 & Mockito
- **Tài liệu API**: springdoc-openapi (Swagger UI tại `http://localhost:8080/swagger-ui.html`)
- **Đóng gói & Khởi chạy**: Dockerfile + docker-compose.yml (Maven Wrapper `./mvnw`)

---

## 3. Cấu trúc thư mục dự án
Dự án tổ chức tuân thủ nguyên tắc **tách 3 lớp** và phụ thuộc một chiều trong Spring Boot (`Controllers/Thymeleaf -> Services -> Repositories`):

```text
smart-crm-technician-scheduling/
├── docs/                                  # Tài liệu kỹ thuật & Sơ đồ thiết kế
│   ├── srs.md                             # Bản đặc tả yêu cầu rút gọn (SRS)
│   ├── architecture.drawio                # Sơ đồ kiến trúc phân lớp
│   ├── usecase.drawio                     # Sơ đồ Use Case (UML)
│   ├── erd.drawio                         # Sơ đồ ERD (Chuẩn 3NF + Legend)
│   ├── schema.sql                         # Mã SQL DDL (MySQL 8.0)
│   ├── wireframe.drawio                   # Giao diện Wireframe 3 màn hình
│   ├── thay-doi-thiet-ke.md              # Nhật ký thay đổi thiết kế theo BT1
│   └── ai-disclosure.md                   # Bảng khai báo sử dụng công cụ AI
├── src/
│   ├── main/
│   │   ├── java/com/mekongmobile/smartcrm/
│   │   │   ├── controllers/               # TẦNG 1: Web UI (Thymeleaf) & REST Controllers
│   │   │   ├── services/                  # TẦNG 2: Logic nghiệp vụ (QT-04, QT-07, QT-08)
│   │   │   ├── repositories/              # TẦNG 3: Spring Data JPA Repositories (MySQL)
│   │   │   ├── entities/                  # Thực thể JPA mapping với MySQL Tables
│   │   │   ├── dto/                       # Data Transfer Objects & Validators (Tầng 1)
│   │   │   ├── exceptions/                # AppException & GlobalExceptionHandler
│   │   │   └── SmartCrmApplication.java   # Class khởi chạy Spring Boot
│   │   └── resources/
│   │       ├── templates/                 # Giao diện Thymeleaf HTML
│   │       ├── static/                    # CSS, JS, Images
│   │       └── application.yml            # Cấu hình Spring Boot & MySQL DB
│   └── test/
│       └── java/com/mekongmobile/smartcrm/
│           └── services/                  # Unit Tests (JUnit 5 + Mockito)
├── Dockerfile                             # Cấu hình đóng gói ứng dụng Java Spring Boot
├── docker-compose.yml                     # Khởi chạy MySQL 8 + Spring Boot App
├── pom.xml                                # Quản lý thư viện Maven (Dependencies)
├── mvnw / mvnw.cmd                        # Maven Wrapper (chạy lệnh không cần cài Maven)
├── .env.example                           # Mẫu khai báo biến môi trường
├── .gitignore                             # Bỏ qua target/, .idea/, .env, *.log
└── README.md                              # Hướng dẫn khởi chạy & Trạng thái dự án
6. Trạng thái hiện tại (Cập nhật Buổi 8 / Sprint 1)
[x] Đọc phản hồi BT1 và ghi nhận vào docs/thay-doi-thiet-ke.md [TD-01 đến TD-04].
[x] Dựng cấu trúc thư mục src/ ánh xạ 1-1 với sơ đồ kiến trúc phân lớp.
[x] Khởi tạo mã SQL DDL migration docs/schema.sql hỗ trợ MySQL 8.0.
[x] Hiện thực module cốt lõi CRUD /tickets qua 3 lớp (routes, services, repositories).
[x] Bổ sung bảng device, cột technician.full_name và tệp wireframe.drawio vào docs/.
[ ] Xây dựng quy tắc nghiệp vụ QT-07 (lý do đổi KTV) và QT-08 (kiểm tra tay nghề).
[ ] Hoàn thiện đủ 3/3 Unit Test thuộc 3 loại khác nhau (hiện mới có 1/3 test luồng đúng).
[ ] Tạo tag v0.2 chuẩn bị nộp Bài tập 2 (hạn nộp Buổi 12).
