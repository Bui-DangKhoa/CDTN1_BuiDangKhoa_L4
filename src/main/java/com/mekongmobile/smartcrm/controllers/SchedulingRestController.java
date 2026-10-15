package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.ApiResponse;
import com.mekongmobile.smartcrm.dto.ScheduleAppointmentDto;
import com.mekongmobile.smartcrm.dto.TechnicianDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.services.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TẦNG 1: REST API Controller cho nghiệp vụ điều phối kỹ thuật viên (QT-07).
 */
@RestController
@RequestMapping("/api/v1/scheduling")
@RequiredArgsConstructor
public class SchedulingRestController {

    private final SchedulingService schedulingService;

    /**
     * API tra cứu danh sách kỹ thuật viên khả dụng và đủ kỹ năng cho ticket tại thời điểm hẹn.
     */
    @GetMapping("/eligible-technicians")
    public ResponseEntity<ApiResponse<List<TechnicianDto>>> getEligibleTechnicians(
            @RequestParam("ticketId") Long ticketId,
            @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentTime) {
        List<TechnicianDto> technicians = schedulingService.findEligibleTechnicians(ticketId, appointmentTime);
        return ResponseEntity.ok(ApiResponse.of("Tìm thấy " + technicians.size() + " kỹ thuật viên phù hợp", technicians));
    }

    /**
     * QT-07: API thực hiện điều phối lịch hẹn kỹ thuật viên.
     */
    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<TicketResponseDto>> scheduleAppointment(
            @Valid @RequestBody ScheduleAppointmentDto scheduleDto) {
        TicketResponseDto responseDto = schedulingService.scheduleAppointment(scheduleDto);
        return ResponseEntity.ok(ApiResponse.of("Điều phối lịch hẹn kỹ thuật viên thành công (QT-07)", responseDto));
    }
}
