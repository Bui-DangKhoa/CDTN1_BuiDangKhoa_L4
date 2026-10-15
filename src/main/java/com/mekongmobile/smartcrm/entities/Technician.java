package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Thực thể Kỹ thuật viên (technician).
 */
@Entity
@Table(name = "technician")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "employee_id", nullable = false, unique = true)
    private Long employeeId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "level", nullable = false, length = 15)
    private String level;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
