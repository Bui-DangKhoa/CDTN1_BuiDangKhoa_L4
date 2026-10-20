package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.LoginRequest;
import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.AppUser;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TẦNG 2: Dịch vụ xác thực danh tính người dùng nội bộ (AuthService).
 * Kiểm tra mật khẩu mã hóa BCrypt và thiết lập phiên làm việc UserSessionDto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    /**
     * Xác thực thông tin đăng nhập từ LoginRequest.
     * Đối sánh mật khẩu bằng chuẩn BCrypt.
     *
     * @param request thông tin gồm username và password
     * @return UserSessionDto thông tin người dùng được lưu vào session
     * @throws AppException("INVALID_CREDENTIALS", ...) khi thông tin đăng nhập không hợp lệ
     */
    @Transactional(readOnly = true)
    public UserSessionDto login(LoginRequest request) {
        log.info("Thực hiện xác thực đăng nhập cho tài khoản: {}", request.getUsername());

        AppUser user = userRepository.findByUsernameAndIsActiveTrue(request.getUsername())
                .orElseThrow(() -> new AppException("INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng"));

        boolean matches = false;
        try {
            matches = BCrypt.checkpw(request.getPassword(), user.getPassword());
        } catch (Exception ex) {
            log.error("Lỗi đối sánh mật khẩu BCrypt cho user {}: {}", request.getUsername(), ex.getMessage());
            throw new AppException("INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if (!matches) {
            log.warn("Đăng nhập thất bại: Mật khẩu không chính xác cho user: {}", request.getUsername());
            throw new AppException("INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        log.info("Đăng nhập thành công cho user: {}, role: {}, centerId: {}",
                user.getUsername(), user.getRole(), user.getCenterId());

        return UserSessionDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .centerId(user.getCenterId())
                .build();
    }
}
