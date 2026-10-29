package com.mekongmobile.smartcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cập nhật thông tin phiếu bảo hành (Ticket PUT).
 * Chỉ cho phép cập nhật: mô tả sự cố, độ ưu tiên, danh mục sự cố, số điện thoại khách hàng.
 * Không cho phép đổi trạng thái hoặc kỹ thuật viên qua PUT (phải qua QT-06/QT-07).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequestDto {

    @NotBlank(message = "Mô tả sự cố không được để trống")
    private String issueDesc;

    @NotBlank(message = "Mức độ ưu tiên không được để trống")
    private String priority;

    @NotNull(message = "Mã danh mục sự cố không được để trống")
    private Integer categoryId;

    private String customerPhone;
}

