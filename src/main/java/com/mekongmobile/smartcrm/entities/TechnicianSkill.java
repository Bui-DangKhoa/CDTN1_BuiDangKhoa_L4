package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Thực thể Kỹ năng kỹ thuật viên theo danh mục sự cố (technician_skill).
 */
@Entity
@Table(name = "technician_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianSkill {

    @EmbeddedId
    private TechnicianSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("technicianId")
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private IssueCategory category;

    @Column(name = "proficiency", nullable = false)
    private Short proficiency;
}
