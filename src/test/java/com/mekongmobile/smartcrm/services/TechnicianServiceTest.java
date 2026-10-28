package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Bộ kiểm thử tự động Unit Test cho TechnicianService.
 * Sử dụng JUnit 5 và Mockito độc lập với cơ sở dữ liệu.
 */
@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @InjectMocks
    private TechnicianServiceImpl technicianService;

    private Technician mockTechnician;
    private UserSessionDto mockUserSession;

    @BeforeEach
    void setUp() {
        mockTechnician = Technician.builder()
                .technicianId(1L)
                .userId(2L)
                .employeeId(1001L)
                .fullName("Trần Kỹ Thuật")
                .centerId(1L)
                .level("BAC_3")
                .isActive(true)
                .build();

        mockUserSession = UserSessionDto.builder()
                .userId(2L)
                .username("ktv1")
                .fullName("Trần Kỹ Thuật")
                .role("KY_THUAT_VIEN")
                .centerId(1L)
                .build();
    }

    @Test
    @DisplayName("Happy Path: Tài khoản người dùng khớp KTV theo user_id -> Trả về đúng KTV")
    void testResolveTechnicianForUser_MatchedUser_ReturnsTechnician() {
        // Given
        when(technicianRepository.findByUserId(2L)).thenReturn(Optional.of(mockTechnician));

        // When
        Optional<Technician> result = technicianService.resolveTechnicianForUser(mockUserSession);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getTechnicianId());
        assertEquals("Trần Kỹ Thuật", result.get().getFullName());
        assertEquals(2L, result.get().getUserId());
        verify(technicianRepository, times(1)).findByUserId(2L);
        verify(technicianRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Negative Path: Tài khoản người dùng không khớp KTV nào -> Trả về rỗng và KHÔNG gọi findById(1L)")
    void testResolveTechnicianForUser_UnmatchedUser_ReturnsEmpty_NeverCallsFindById1L() {
        // Given
        UserSessionDto unknownUser = UserSessionDto.builder()
                .userId(999L)
                .username("unknown_user")
                .fullName("Người Dùng Lạ")
                .role("KY_THUAT_VIEN")
                .centerId(1L)
                .build();

        when(technicianRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When
        Optional<Technician> result = technicianService.resolveTechnicianForUser(unknownUser);

        // Then
        assertTrue(result.isEmpty());
        verify(technicianRepository, times(1)).findByUserId(999L);
        verify(technicianRepository, never()).findById(1L);
        verify(technicianRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Boundary / Null Path: UserSession hoặc userId bị null -> Trả về rỗng an toàn")
    void testResolveTechnicianForUser_NullUserOrUserId_ReturnsEmpty() {
        // When & Then
        assertTrue(technicianService.resolveTechnicianForUser(null).isEmpty());

        UserSessionDto sessionWithoutId = UserSessionDto.builder()
                .username("test")
                .build();
        assertTrue(technicianService.resolveTechnicianForUser(sessionWithoutId).isEmpty());

        verify(technicianRepository, never()).findByUserId(anyLong());
        verify(technicianRepository, never()).findById(anyLong());
    }
}
