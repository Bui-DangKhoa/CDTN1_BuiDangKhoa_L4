package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.ApiResponse;
import com.mekongmobile.smartcrm.dto.ScheduleAppointmentDto;
import com.mekongmobile.smartcrm.dto.TechnicianDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.exceptions.AppException;
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
            @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentTime,
            jakarta.servlet.http.HttpSession session) {
        UserSessionDto manager = requireManager(session);
        List<TechnicianDto> technicians = schedulingService.findEligibleTechnicians(ticketId, appointmentTime, manager.getCenterId());
        return ResponseEntity.ok(ApiResponse.of("Tìm thấy " + technicians.size() + " kỹ thuật viên phù hợp", technicians));
    }

    /**
     * QT-07: API thực hiện điều phối lịch hẹn kỹ thuật viên.
     */
    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<TicketResponseDto>> scheduleAppointment(
            @Valid @RequestBody ScheduleAppointmentDto scheduleDto, jakarta.servlet.http.HttpSession session) {
        UserSessionDto manager = requireManager(session);
        scheduleDto.setScheduledBy(manager.getUserId());
        TicketResponseDto responseDto = schedulingService.scheduleAppointment(scheduleDto, manager.getCenterId());
        return ResponseEntity.ok(ApiResponse.of("Điều phối lịch hẹn kỹ thuật viên thành công (QT-07)", responseDto));
    }

    private UserSessionDto requireManager(jakarta.servlet.http.HttpSession session) {
        UserSessionDto user = session == null ? null : (UserSessionDto) session.getAttribute("currentUser");
        if (user == null || user.getUserId() == null) throw new AppException("UNAUTHORIZED", "Vui lòng đăng nhập");
        if (!"QUAN_LY".equalsIgnoreCase(user.getRole())) throw new AppException("FORBIDDEN", "Chức năng chỉ dành cho quản lý");
        if (user.getCenterId() == null) throw new AppException("FORBIDDEN_OTHER_CENTER", "Không có quyền truy cập dữ liệu này");
        return user;
    }
}
