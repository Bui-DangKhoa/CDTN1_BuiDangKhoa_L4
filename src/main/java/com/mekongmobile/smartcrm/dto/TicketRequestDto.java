package com.mekongmobile.smartcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TẦNG 1: DTO tiếp nhận thông tin yêu cầu tạo mới Ticket (QT-04).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequestDto {

    @NotBlank(message = "Mã ticket không được để trống")
    private String ticketCode;

    @NotNull(message = "Mã khách hàng không được để trống")
    private Long customerId;

    @NotNull(message = "Mã thiết bị không được để trống")
    private Long deviceId;

    @NotNull(message = "Mã trung tâm dịch vụ không được để trống")
    private Long centerId;

    @NotNull(message = "Mã danh mục sự cố không được để trống")
    private Integer categoryId;

    @NotBlank(message = "Mô tả sự cố không được để trống")
    private String issueDesc;

    // Ưu tiên (có thể null, nếu null sẽ tự động lấy theo default_priority của danh mục)
    private String priority;

    // Thời hạn hoàn thành mong muốn (nếu null, hệ thống tự động tính theo SLA)
    private LocalDateTime dueDate;
}
