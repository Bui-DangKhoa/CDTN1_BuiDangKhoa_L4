package com.mekongmobile.smartcrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TẦNG 1: DTO hiển thị dòng lịch sử sửa chữa của thiết bị (Khung 3 - Màn hình KTV).
 * Phục vụ US5 / FR5 theo Wireframe MH3.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRepairHistoryDto {

    /** Mã phiếu sửa chữa trước đây (vd: #BH-00071). */
    private String ticketCode;

    /** Ngày sửa chữa / hoàn tất (dd/MM/yyyy). */
    private String repairedDate;

    /** Họ tên Kỹ thuật viên phụ trách sửa chữa trước đó. */
    private String technicianName;

    /** Nội dung xử lý kỹ thuật / giải pháp khắc phục. */
    private String solutionDesc;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Định dạng ngày sửa chữa: ưu tiên closed_at (thời điểm hoàn tất/đóng), fallback sang received_at.
     */
    public static String formatRepairedDate(LocalDateTime closedAt, LocalDateTime receivedAt) {
        LocalDateTime target = (closedAt != null) ? closedAt : receivedAt;
        return (target != null) ? target.format(DATE_FORMATTER) : "—";
    }
}
