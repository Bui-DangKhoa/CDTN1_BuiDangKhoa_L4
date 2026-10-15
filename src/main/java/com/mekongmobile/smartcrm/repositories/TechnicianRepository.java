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

    Optional<Technician> findByEmployeeId(Long employeeId);

    List<Technician> findByCenterIdAndIsActiveTrue(Long centerId);

    /**
     * Tìm các kỹ thuật viên thuộc trung tâm có kỹ năng xử lý danh mục sự cố được chỉ định.
     */
    @Query("""
        SELECT t FROM Technician t
        JOIN TechnicianSkill ts ON ts.technician.technicianId = t.technicianId
        WHERE t.centerId = :centerId
          AND t.isActive = true
          AND ts.category.categoryId = :categoryId
        ORDER BY ts.proficiency DESC
    """)
    List<Technician> findEligibleTechnicians(@Param("centerId") Long centerId, @Param("categoryId") Integer categoryId);
}
