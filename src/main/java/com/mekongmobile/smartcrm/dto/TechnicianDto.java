package com.mekongmobile.smartcrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TẦNG 1: DTO thông tin Kỹ thuật viên hiển thị trên giao diện điều phối.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianDto {

    private Long technicianId;
    private Long employeeId;
    private String fullName;
    private Long centerId;
    private String level;
    private Boolean isActive;
}
