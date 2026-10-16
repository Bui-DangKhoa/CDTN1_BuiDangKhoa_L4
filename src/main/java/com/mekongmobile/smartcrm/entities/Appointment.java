package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể Lịch hẹn kỹ thuật viên (appointment).
 * <p>
 * QT-L4-01 / NFR2: CSDL có cột sinh {@code active_tech_time} (= appointment_time nếu status &lt;&gt; 'DA_HUY')
 * cùng UNIQUE INDEX (technician_id, active_tech_time) làm lớp chặn cuối cùng chống trùng lịch.
 * Cột sinh này cố ý KHÔNG ánh xạ vào entity để Hibernate không ghi đè định nghĩa GENERATED.
 */
@Entity
@Table(name = "appointment",
        indexes = @Index(name = "fk_app_ticket", columnList = "ticket_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long appointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    /** Trạng thái: CHO_XAC_NHAN (mặc định), ..., DA_HUY (không tính trùng lịch). */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "CHO_XAC_NHAN";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
