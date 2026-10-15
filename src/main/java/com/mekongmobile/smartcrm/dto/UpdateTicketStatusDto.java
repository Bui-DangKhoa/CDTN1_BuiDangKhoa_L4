package com.mekongmobile.smartcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TẦNG 1: DTO cập nhật trạng thái tiến độ Ticket (QT-08).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStatusDto {

    @NotBlank(message = "Trạng thái mới không được để trống")
    private String newStatus;

    @NotNull(message = "Mã nhân viên thực hiện không được để trống")
    private Long changedBy;

    private String changeReason;
}
