package com.mekongmobile.smartcrm.repositories;

import com.mekongmobile.smartcrm.entities.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TẦNG 3: Repository quản lý dữ liệu Kỹ thuật viên (Technician).
 */
@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByUserId(Long userId);

    Optional<Technician> findByEmployeeId(Long employeeId);

    Optional<Technician> findByFullName(String fullName);

    List<Technician> findByCenterIdAndIsActiveTrue(Long centerId);

    /**
     * QT-08: Lọc danh sách KTV đủ điều kiện gán cho một phiếu.
     * <ul>
     *   <li>Cùng {@code center_id} với phiếu.</li>
     *   <li>Có kỹ năng thuộc {@code category_id} của phiếu với {@code proficiency >= 3}.</li>
     *   <li>Đang hoạt động ({@code is_active = true}).</li>
     * </ul>
     * Kết quả sắp xếp theo tay nghề giảm dần, rồi theo họ tên.
     */
    @Query("""
        SELECT t FROM Technician t
        JOIN TechnicianSkill ts ON ts.technician = t
        WHERE t.centerId = :centerId
          AND t.isActive = true
          AND ts.category.categoryId = :categoryId
          AND ts.proficiency >= 3
        ORDER BY ts.proficiency DESC, t.fullName ASC
    """)
    List<Technician> findEligibleTechnicians(@Param("centerId") Long centerId, @Param("categoryId") Integer categoryId);
}
