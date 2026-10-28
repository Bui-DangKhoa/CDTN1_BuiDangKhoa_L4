package com.mekongmobile.smartcrm.config;

import com.mekongmobile.smartcrm.entities.AppUser;
import com.mekongmobile.smartcrm.entities.Appointment;
import com.mekongmobile.smartcrm.entities.Device;
import com.mekongmobile.smartcrm.entities.IssueCategory;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.repositories.AppointmentRepository;
import com.mekongmobile.smartcrm.repositories.DeviceRepository;
import com.mekongmobile.smartcrm.repositories.IssueCategoryRepository;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import com.mekongmobile.smartcrm.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Tự động nạp tài khoản mẫu kiểm thử nếu bảng app_user chưa có dữ liệu.
 * Tự động sửa chữa lỗi mã hóa (charset/mojibake) cho danh mục sự cố, KTV và phiếu bảo hành.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final IssueCategoryRepository issueCategoryRepository;
    private final TechnicianRepository technicianRepository;
    private final TicketRepository ticketRepository;
    private final DeviceRepository deviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        seedDefaultUsers();
        repairCorruptedData();
        seedLookupDemoData();
    }

    private void seedDefaultUsers() {
        if (userRepository.count() == 0) {
            log.info("Bảng app_user đang trống. Tiến hành tự động nạp tài khoản mẫu kiểm thử...");

            String hashedPassword = BCrypt.hashpw("password123", BCrypt.gensalt(10));

            AppUser manager = AppUser.builder()
                    .username("quanly1")
                    .password(hashedPassword)
                    .fullName("Nguyễn Quản Lý")
                    .role("QUAN_LY")
                    .centerId(1L)
                    .isActive(true)
                    .build();

            AppUser technician = AppUser.builder()
                    .username("ktv1")
                    .password(hashedPassword)
                    .fullName("Trần Kỹ Thuật")
                    .role("KY_THUAT_VIEN")
                    .centerId(1L)
                    .isActive(true)
                    .build();

            userRepository.save(manager);
            AppUser savedTechUser = userRepository.save(technician);

            // Liên kết tài khoản ktv1 với Technician cùng họ tên (nếu bảng technician đã có sẵn dữ liệu)
            technicianRepository.findByFullName(savedTechUser.getFullName()).ifPresent(tech -> {
                if (tech.getUserId() == null) {
                    tech.setUserId(savedTechUser.getUserId());
                    technicianRepository.save(tech);
                }
            });

            log.info("Đã tự động khởi tạo thành công tài khoản mẫu: 'quanly1' và 'ktv1' (Mật khẩu: 'password123')");
        }
    }

    private void repairCorruptedData() {
        try {
            // Chuyển đổi bảng sang utf8mb4 nếu đang ở bảng mã cũ
            jdbcTemplate.execute("ALTER TABLE `issue_category` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            jdbcTemplate.execute("ALTER TABLE `ticket` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            jdbcTemplate.execute("ALTER TABLE `technician` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            jdbcTemplate.execute("ALTER TABLE `app_user` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (Exception e) {
            log.debug("Bỏ qua ALTER TABLE charset (có thể do môi trường test/h2/quyền hạn): {}", e.getMessage());
        }

        // Sửa lỗi mã hóa tiếng Việt trong danh mục sự cố kỹ thuật (issue_category)
        repairCategory(1, "Thay màn hình cảm ứng", "CAO");
        repairCategory(2, "Thay pin / Lỗi IC nguồn", "CAO");
        repairCategory(3, "Lỗi phần mềm / Treo hệ điều hành", "TRUNG_BINH");
        repairCategory(4, "Bảo dưỡng / Vệ sinh máy & Tra keo", "THAP");
        repairCategory(5, "Sửa chữa Camera & Cụm cảm biến", "TRUNG_BINH");

        // Sửa lỗi mã hóa họ tên Kỹ thuật viên (technician)
        repairTechnician(1L, "Trần Kỹ Thuật");
        repairTechnician(2L, "Lê Văn An");
        repairTechnician(3L, "Phạm Hoàng Bách");
        repairTechnician(4L, "Đỗ Minh Tuấn");

        // Sửa lỗi mã hóa mô tả lỗi phiếu bảo hành (ticket)
        repairTicketDesc(101L, "Màn hình bị sọc xanh và loạn cảm ứng sau va đập mạnh");
        repairTicketDesc(102L, "Pin phù kênh nắp lưng, máy tự sập nguồn đột ngột khi dưới 30%");
        repairTicketDesc(103L, "Máy bị treo táo sau khi nâng cấp hệ điều hành macOS");
        repairTicketDesc(104L, "Bảo dưỡng định kỳ, kiểm tra chân sạc USB-C lỏng lẻo");
        repairTicketDesc(105L, "Camera sau bị rung và mờ, không lấy nét quang học được");
        repairTicketDesc(106L, "Màn hình OLED bị ố vàng góc trên, cảm ứng đơ giật ngắt quãng");
        repairTicketDesc(107L, "Pin chai còn 68%, máy thường xuyên báo pin ảo và tự khởi động lại");
        repairTicketDesc(108L, "Cụm camera tele zoom 5x bị mờ sương và kẹt chống rung OIS");

        // Sửa lỗi mã hóa họ tên tài khoản người dùng
        userRepository.findByUsername("quanly1").ifPresent(u -> {
            if (!"Nguyễn Quản Lý".equals(u.getFullName())) {
                u.setFullName("Nguyễn Quản Lý");
                userRepository.save(u);
            }
        });
        userRepository.findByUsername("ktv1").ifPresent(u -> {
            if (!"Trần Kỹ Thuật".equals(u.getFullName())) {
                u.setFullName("Trần Kỹ Thuật");
                userRepository.save(u);
            }
        });
    }

    private void repairCategory(Integer id, String expectedName, String defaultPriority) {
        issueCategoryRepository.findById(id).ifPresentOrElse(cat -> {
            if (!expectedName.equals(cat.getCategoryName())) {
                log.info("Tự động sửa lỗi mã hóa danh mục #{}: '{}' -> '{}'", id, cat.getCategoryName(), expectedName);
                cat.setCategoryName(expectedName);
                cat.setDefaultPriority(defaultPriority);
                issueCategoryRepository.save(cat);
            }
        }, () -> {
            IssueCategory newCat = IssueCategory.builder()
                    .categoryId(id)
                    .categoryName(expectedName)
                    .defaultPriority(defaultPriority)
                    .isActive(true)
                    .build();
            issueCategoryRepository.save(newCat);
        });
    }

    private void repairTechnician(Long id, String expectedName) {
        technicianRepository.findById(id).ifPresent(tech -> {
            if (!expectedName.equals(tech.getFullName())) {
                log.info("Tự động sửa lỗi mã hóa KTV #{}: '{}' -> '{}'", id, tech.getFullName(), expectedName);
                tech.setFullName(expectedName);
                technicianRepository.save(tech);
            }
        });
    }

    private void repairTicketDesc(Long id, String expectedDesc) {
        ticketRepository.findById(id).ifPresent(t -> {
            if (!expectedDesc.equals(t.getIssueDesc())) {
                log.info("Tự động sửa lỗi mã hóa mô tả sự cố ticket #{}: '{}' -> '{}'", id, t.getIssueDesc(), expectedDesc);
                t.setIssueDesc(expectedDesc);
                ticketRepository.save(t);
            }
        });
    }

    private void seedLookupDemoData() {
        if (ticketRepository.findByTicketCode("BH-00125").isEmpty()) {
            log.info("Khởi tạo dữ liệu mẫu phiếu bảo hành BH-00125 cho cổng tra cứu khách hàng (/lookup)...");
            Device device = deviceRepository.findById(1L).orElse(null);
            IssueCategory category = issueCategoryRepository.findById(1).orElse(null);
            Technician technician = technicianRepository.findById(1L).orElse(null);

            if (device != null && category != null && technician != null) {
                Ticket ticket = Ticket.builder()
                        .ticketId(125L)
                        .ticketCode("BH-00125")
                        .customerId(201L)
                        .customerPhone("0901234567")
                        .device(device)
                        .centerId(1L)
                        .technician(technician)
                        .category(category)
                        .issueDesc("Màn hình cảm ứng bị loạn, hẹn tiếp nhận kiểm tra và thay màn hình")
                        .priority("CAO")
                        .status("DANG_XU_LY")
                        .receivedAt(LocalDateTime.now().minusDays(1))
                        .dueDate(LocalDateTime.now().plusDays(2))
                        .build();
                ticket = ticketRepository.save(ticket);

                if (appointmentRepository.findByTicket_TicketId(ticket.getTicketId()).isEmpty()) {
                    Appointment appointment = Appointment.builder()
                            .appointmentId(125L)
                            .ticket(ticket)
                            .technician(technician)
                            .appointmentTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0))
                            .status("CHO_XAC_NHAN")
                            .createdAt(LocalDateTime.now().minusDays(1))
                            .build();
                    appointmentRepository.save(appointment);
                    log.info("Đã tạo lịch hẹn mẫu thành công cho phiếu BH-00125 (SĐT: 0901234567)");
                }
            }
        }
    }
}
