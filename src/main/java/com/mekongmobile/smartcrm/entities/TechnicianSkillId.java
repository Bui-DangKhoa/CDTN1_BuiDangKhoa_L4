package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Key cho bảng technician_skill.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianSkillId implements Serializable {

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "category_id")
    private Integer categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechnicianSkillId that = (TechnicianSkillId) o;
        return Objects.equals(technicianId, that.technicianId) &&
               Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technicianId, categoryId);
    }
}
