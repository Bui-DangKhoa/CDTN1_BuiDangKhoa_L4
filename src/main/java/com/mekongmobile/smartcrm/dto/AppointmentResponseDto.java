package com.mekongmobile.smartcrm.dto;

import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TẦNG 1: Response DTO thông tin Lịch hẹn trả về cho Web UI / REST Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {

    private Long appointmentId;
    private Long ticketId;
    private String ticketCode;
    private Long technicianId;
    private String technicianName;
    private LocalDateTime appointmentTime;
    private String status;
    private LocalDateTime createdAt;

    /** QT-15: Số điện thoại khách hàng đã che qua MaskingUtil (dạng 090****567) */
    private String customerPhone;
    private String deviceName;
    private String issueDesc;

    /** Giữ quan hệ với Ticket & Technician phục vụ view Thymeleaf */
    private Ticket ticket;
    private Technician technician;
}
