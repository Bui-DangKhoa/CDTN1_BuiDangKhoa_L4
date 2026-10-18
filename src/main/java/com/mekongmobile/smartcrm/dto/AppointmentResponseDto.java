package com.mekongmobile.smartcrm.dto;

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
}
