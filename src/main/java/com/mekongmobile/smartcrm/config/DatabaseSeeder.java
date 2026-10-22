package com.mekongmobile.smartcrm.config;

import com.mekongmobile.smartcrm.entities.AppUser;
import com.mekongmobile.smartcrm.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Tự động nạp tài khoản mẫu kiểm thử nếu bảng app_user chưa có dữ liệu.
 * Đảm bảo các tài khoản 'quanly1' và 'ktv1' luôn tồn tại và đăng nhập được trên mọi môi trường khởi chạy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
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
            userRepository.save(technician);

            log.info("Đã tự động khởi tạo thành công tài khoản mẫu: 'quanly1' và 'ktv1' (Mật khẩu: 'password123')");
        }
    }
}
