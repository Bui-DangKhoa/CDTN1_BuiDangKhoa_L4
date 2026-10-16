package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Thực thể Kỹ năng kỹ thuật viên theo danh mục sự cố (technician_skill).
 * Khóa chính phức hợp (technician_id, category_id) qua {@link TechnicianSkillId}.
 */
@Entity
@Table(name = "technician_skill",
        indexes = @Index(name = "fk_skill_cat", columnList = "category_id"))
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

    /** Mức tay nghề 1–5; QT-08 yêu cầu &gt;= 3 để được gán phiếu thuộc nhóm sự cố này. */
    @NotNull
    @Min(1)
    @Max(5)
    @Column(name = "proficiency", nullable = false)
    private Short proficiency;
}
