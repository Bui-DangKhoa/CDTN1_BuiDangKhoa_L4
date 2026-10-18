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

    @Size(min = 10, message = "Lý do thay đổi KTV phải tối thiểu 10 ký tự")
    private String changeReason;
}
