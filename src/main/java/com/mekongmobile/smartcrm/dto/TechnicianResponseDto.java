package com.mekongmobile.smartcrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TẦNG 1: Response DTO thông tin Kỹ thuật viên trả về cho Web UI / REST Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianResponseDto {

    private Long technicianId;
    private Long employeeId;
    private String fullName;
    private Long centerId;
    private String level;
    private Boolean isActive;
    private Integer proficiency;
    private Integer activeTicketsCount;
}
