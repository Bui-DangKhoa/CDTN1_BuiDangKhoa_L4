package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể Phiếu yêu cầu sửa chữa/bảo hành (ticket).
 */
@Entity
@Table(name = "ticket", indexes = {
        @Index(name = "idx_ticket_status_due", columnList = "status, due_date"),
        @Index(name = "idx_ticket_tech_due", columnList = "technician_id, due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "ticket_code", nullable = false, length = 20, unique = true)
    private String ticketCode;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    /** KTV phụ trách – có thể NULL khi phiếu chưa được phân công. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private IssueCategory category;

    @Column(name = "issue_desc", nullable = false, columnDefinition = "TEXT")
    private String issueDesc;

    @Column(name = "resolution", length = 500)
    private String resolution;

    @Column(name = "priority", nullable = false, length = 10)
    private String priority;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "MOI";

    @Column(name = "received_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
