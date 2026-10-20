package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.LoginRequest;
import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.AppUser;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("TC-AUTH-01: Đăng nhập thành công với mật khẩu chính xác (BCrypt)")
    void testLoginSuccess() {
        String rawPassword = "password123";
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));

        AppUser mockUser = AppUser.builder()
                .userId(1L)
                .username("quanly1")
                .password(hash)
                .fullName("Nguyễn Quản Lý")
                .role("QUAN_LY")
                .centerId(1L)
                .isActive(true)
                .build();

        when(userRepository.findByUsernameAndIsActiveTrue("quanly1")).thenReturn(Optional.of(mockUser));

        LoginRequest request = LoginRequest.builder()
                .username("quanly1")
                .password("password123")
                .build();

        UserSessionDto session = authService.login(request);

        assertNotNull(session);
        assertEquals(1L, session.getUserId());
        assertEquals("quanly1", session.getUsername());
        assertEquals("QUAN_LY", session.getRole());
        assertEquals(1L, session.getCenterId());
    }

    @Test
    @DisplayName("TC-AUTH-02: Đăng nhập thất bại khi sai mật khẩu -> Ném AppException INVALID_CREDENTIALS")
    void testLoginWrongPassword() {
        String hash = BCrypt.hashpw("password123", BCrypt.gensalt(10));

        AppUser mockUser = AppUser.builder()
                .userId(2L)
                .username("ktv1")
                .password(hash)
                .fullName("Trần Kỹ Thuật")
                .role("KY_THUAT_VIEN")
                .centerId(1L)
                .isActive(true)
                .build();

        when(userRepository.findByUsernameAndIsActiveTrue("ktv1")).thenReturn(Optional.of(mockUser));

        LoginRequest request = LoginRequest.builder()
                .username("ktv1")
                .password("wrongpassword")
                .build();

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
        assertEquals("Tên đăng nhập hoặc mật khẩu không đúng", ex.getMessage());
    }

    @Test
    @DisplayName("TC-AUTH-03: Đăng nhập thất bại khi tài khoản không tồn tại -> Ném AppException INVALID_CREDENTIALS")
    void testLoginUnknownUser() {
        when(userRepository.findByUsernameAndIsActiveTrue("unknown")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
                .username("unknown")
                .password("password123")
                .build();

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
    }
}
