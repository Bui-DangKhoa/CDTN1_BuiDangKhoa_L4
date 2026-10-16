package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Thực thể Thiết bị của khách hàng (device).
 */
@Entity
@Table(name = "device",
        indexes = @Index(name = "idx_device_customer", columnList = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    /** QT-03: định danh thiết bị duy nhất qua serial_no. */
    @Column(name = "serial_no", nullable = false, length = 50, unique = true)
    private String serialNo;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "warranty_months", nullable = false)
    @Builder.Default
    private Short warrantyMonths = 12;
}
