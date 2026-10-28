package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.Technician;

import java.util.List;
import java.util.Optional;

/**
 * TẦNG 2: Giao diện Service quản lý thông tin Kỹ thuật viên (Technician).
 * Tuân thủ quy tắc tách lớp (C2, S2): Controllers không được truy cập trực tiếp Repository.
 */
public interface TechnicianService {

    Optional<Technician> findById(Long id);

    Optional<Technician> findByUserId(Long userId);

    Optional<Technician> findByFullName(String fullName);

    List<Technician> findActiveTechniciansByCenter(Long centerId);

    /**
     * Xác định Kỹ thuật viên tương ứng với người dùng đăng nhập hiện tại bằng user_id.
     * Trả về Optional.empty() nếu tài khoản không liên kết với KTV nào.
     */
    Optional<Technician> resolveTechnicianForUser(UserSessionDto currentUser);
}
