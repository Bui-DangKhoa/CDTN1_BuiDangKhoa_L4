package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/**
 * Kiểm thử hồi quy cho lỗi Critical đã sửa ở commit af86e4d:
 * Tài khoản chưa liên kết KTV bị fallback nhầm về findById(1L), dẫn đến việc thấy phiếu của KTV khác.
 */
@ExtendWith(MockitoExtension.class)
class TechnicianAccessRegressionTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @InjectMocks
    private TechnicianServiceImpl technicianService;

    @Test
    @DisplayName("Hồi quy bug Critical (af86e4d): Tài khoản chưa liên kết KTV (userId=99) không được fallback về KTV 1L mà phải trả về Optional.empty()")
    void resolveTechnicianForUser_userId99KhongKhopKtv_traVeEmpty() {
        // Given: Giả lập KTV 1 tồn tại trong CSDL (đối tượng bị fallback nhầm trước khi sửa)
        Technician technician1 = Technician.builder()
                .technicianId(1L)
                .employeeId(1001L)
                .fullName("Trần Kỹ Thuật")
                .centerId(1L)
                .level("BAC_3")
                .isActive(true)
                .build();

        // Tài khoản người dùng đăng nhập có userId = 99 chưa liên kết với KTV nào
        UserSessionDto user = UserSessionDto.builder()
                .userId(99L)
                .username("user_chua_lien_ket")
                .fullName("Người Dùng Chưa Gán KTV")
                .role("KY_THUAT_VIEN")
                .centerId(1L)
                .build();

        // Stub lenient chỉ sử dụng findById và findByFullName để tương thích hồi quy
        lenient().when(technicianRepository.findById(1L)).thenReturn(Optional.of(technician1));
        lenient().when(technicianRepository.findById(99L)).thenReturn(Optional.empty());
        lenient().when(technicianRepository.findByFullName(user.getFullName())).thenReturn(Optional.empty());

        // When: Gọi resolveTechnicianForUser với tài khoản chưa liên kết
        Optional<Technician> result = technicianService.resolveTechnicianForUser(user);

        // Then: Kết quả phải rỗng (Optional.empty()), không được fallback về KTV 1
        assertTrue(result.isEmpty(), "resolveTechnicianForUser(user).isEmpty() phải là true khi userId=99 không khớp KTV nào");
    }
}

