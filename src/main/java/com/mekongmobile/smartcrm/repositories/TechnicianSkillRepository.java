package com.mekongmobile.smartcrm.repositories;

import com.mekongmobile.smartcrm.entities.TechnicianSkill;
import com.mekongmobile.smartcrm.entities.TechnicianSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TẦNG 3: Repository quản lý kỹ năng kỹ thuật viên (TechnicianSkill).
 */
@Repository
public interface TechnicianSkillRepository extends JpaRepository<TechnicianSkill, TechnicianSkillId> {

    List<TechnicianSkill> findById_TechnicianId(Long technicianId);

    List<TechnicianSkill> findById_CategoryId(Integer categoryId);
}
