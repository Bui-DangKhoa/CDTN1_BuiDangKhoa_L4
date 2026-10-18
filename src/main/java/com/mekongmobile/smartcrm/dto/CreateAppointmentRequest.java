package com.mekongmobile.smartcrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * TẦNG 1: Request DTO đặt lịch hẹn cho Kỹ thuật viên (QT-L4-01 / NFR2).
 * Lớp chặn kiểm thực dữ liệu đầu vào (Validation Layer 1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotNull(message = "ID phiếu yêu cầu không được để trống")
    private Long ticketId;

    @NotNull(message = "Vui lòng chọn kỹ thuật viên")
    private Long technicianId;

    @NotNull(message = "Thời gian hẹn không được để trống")
    @Future(message = "Thời gian hẹn phải ở trong tương lai")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime appointmentTime;
}
