package com.mekongmobile.smartcrm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TẦNG 1: Request DTO phân công / thay đổi Kỹ thuật viên (QT-07, QT-08).
 * Lớp chặn kiểm thực dữ liệu đầu vào (Validation Layer 1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTechnicianRequest {

    @NotNull(message = "Vui lòng chọn kỹ thuật viên")
    private Long technicianId;

    /**
     * Lý do thay đổi KTV (Quy tắc QT-07: Chỉ bắt buộc >= 10 ký tự khi thay đổi KTV cũ).
     * Kiểm tra động tại Tầng Service (TicketServiceImpl) và JavaScript (app.js).
     */
    private String changeReason;

    /** Ngày hẹn giao – nhận máy (Phần 2 của Modal MH2 – FR3 / QT-L4-01). */
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate appointmentDate;

    /** Khung giờ hẹn dạng "HH:mm - HH:mm" (vd: "14:00 - 15:00"); giờ bắt đầu được dùng làm appointment_time. */
    private String timeSlot;

    /** Nguồn submit form: "modal" (MH1 tích hợp MH2) hoặc rỗng (trang assign-form độc lập). */
    private String source;

    /**
     * Ghép {@code appointmentDate} + giờ bắt đầu của {@code timeSlot} thành thời điểm hẹn.
     *
     * @return thời điểm hẹn, hoặc {@code null} nếu form không gửi thông tin lịch hẹn
     */
    public java.time.LocalDateTime resolveAppointmentTime() {
        if (appointmentDate == null || timeSlot == null || timeSlot.isBlank()) {
            return null;
        }
        String start = timeSlot.split("-")[0].trim();
        return appointmentDate.atTime(java.time.LocalTime.parse(start));
    }
}
