package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể Phiếu yêu cầu sửa chữa/bảo hành (ticket).
 */
@Entity
@Table(name = "ticket")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private IssueCategory category;

    @Column(name = "issue_desc", nullable = false, columnDefinition = "TEXT")
    private String issueDesc;

    @Column(name = "priority", nullable = false, length = 10)
    private String priority;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "MOI";

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
