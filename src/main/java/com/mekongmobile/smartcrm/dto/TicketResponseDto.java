package com.mekongmobile.smartcrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TẦNG 1: DTO phản hồi dữ liệu chi tiết Ticket cho Web UI / REST Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDto {

    private Long ticketId;
    private String ticketCode;
    private Long customerId;
    private String customerPhone;
    private Long deviceId;
    private String deviceName;
    private String serialNo;
    private Long centerId;
    private Long technicianId;
    private String technicianName;
    private Integer categoryId;
    private String categoryName;
    private String issueDesc;
    private String priority;
    private String status;
    private LocalDateTime receivedAt;
    private LocalDateTime dueDate;
    private LocalDateTime closedAt;
    private String slaStatus;
    private Integer repeatCount;

    public String getSlaStatus() {
        if (slaStatus != null) {
            return slaStatus;
        }
        if (dueDate == null) {
            return "CON_HAN";
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(dueDate)) {
            return "QUA_HAN";
        }
        if (!dueDate.isAfter(now.plusHours(24))) {
            return "SAP_QUA_HAN";
        }
        return "CON_HAN";
    }
}
