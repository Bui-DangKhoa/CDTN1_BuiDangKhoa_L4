package com.mekongmobile.smartcrm.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TẦNG 1: DTO gửi yêu cầu điều phối lịch hẹn kỹ thuật viên (QT-07).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAppointmentDto {

    @NotNull(message = "Mã ticket không được để trống")
    private Long ticketId;

    @NotNull(message = "Mã kỹ thuật viên không được để trống")
    private Long technicianId;

    @NotNull(message = "Thời gian hẹn không được để trống")
    @Future(message = "Thời gian hẹn phải nằm trong tương lai")
    private LocalDateTime appointmentTime;

    @NotNull(message = "Mã người thực hiện không được để trống")
    private Long scheduledBy;
}
